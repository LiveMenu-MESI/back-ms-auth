package cloudSecurity.service.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;

/**
 * Service for extracting user information from JWT tokens.
 */
@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.realm")
    String realm;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String clientSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getIntrospectUrl() {
        return keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
    }

    /**
     * Validates token and extracts user email from JWT token.
     * First validates the token, then extracts the email.
     * Returns null if token is invalid or email cannot be extracted.
     */
    public String getUserEmailFromToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            Log.debug("Token is null or blank");
            return null;
        }

        // First, try to decode JWT directly (faster, no HTTP call)
        // But we still need to validate the token signature/expiration
        String email = extractEmailFromJWT(accessToken);
        if (email != null && !email.isBlank()) {
            // JWT decoded successfully, but we MUST validate it's still active
            // because JWT can be decoded even if expired or revoked
            if (!isTokenValid(accessToken)) {
                Log.warn("JWT decoded but token validation failed - token may be expired or revoked");
                return null;
            }
            Log.debugf("Extracted email from JWT: %s", email);
            return email;
        }

        // Fallback: use token introspection (validates and extracts in one call)
        Log.debug("JWT decode failed, trying introspection");
        email = extractEmailFromIntrospection(accessToken);
        if (email != null && !email.isBlank()) {
            Log.debugf("Extracted email from introspection: %s", email);
            return email;
        }

        Log.warn("Could not extract email from token - token may be invalid or email not found");
        return null;
    }

    /**
     * Extracts email from JWT token by decoding the payload.
     * JWT format: header.payload.signature
     */
    private String extractEmailFromJWT(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                Log.debug("JWT does not have 3 parts");
                return null;
            }

            // Decode payload (second part) - JWT uses URL-safe Base64
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Log.debugf("JWT payload decoded: %s", payload);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            
            Log.debugf("JWT claims available: %s", claims.keySet());

            // Try different claim names for email (in order of preference)
            String email = (String) claims.get("email");
            if (email != null && !email.isBlank()) {
                Log.debugf("Found email in 'email' claim: %s", email);
                return email;
            }

            email = (String) claims.get("preferred_username");
            if (email != null && !email.isBlank()) {
                Log.debugf("Found email in 'preferred_username' claim: %s", email);
                return email;
            }

            email = (String) claims.get("username");
            if (email != null && !email.isBlank()) {
                Log.debugf("Found email in 'username' claim: %s", email);
                return email;
            }

            // Try 'sub' if it's an email format (Keycloak often uses email as sub)
            Object subObj = claims.get("sub");
            if (subObj != null) {
                String sub = subObj.toString();
                if (sub.contains("@")) {
                    Log.debugf("Found email in 'sub' claim: %s", sub);
                    return sub;
                }
            }

            Log.warnf("Could not find email in JWT claims. Available claims: %s", claims.keySet());
            return null;
        } catch (IllegalArgumentException e) {
            // Base64 decoding error
            Log.debugf("Base64 decoding error: %s", e.getMessage());
            return null;
        } catch (Exception e) {
            Log.debugf("Error decoding JWT: %s", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extracts email from token introspection response.
     * Note: This method assumes the token has already been validated.
     * It does NOT validate the token again.
     */
    private String extractEmailFromIntrospection(String accessToken) {
        Client client = ClientBuilder.newClient();
        try {
            String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            Form form = new Form().param("token", accessToken);

            Response res = client.target(getIntrospectUrl())
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Basic " + basic)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

            String body = res.readEntity(String.class);
            if (res.getStatus() != 200) {
                Log.debugf("Introspection returned status: %d", res.getStatus());
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(body, Map.class);
            
            // Note: Token validation should have been done before calling this method
            // But we check active status as a safety measure
            if (!Boolean.TRUE.equals(json.get("active"))) {
                Log.warn("Token introspection shows token is not active (should have been validated earlier)");
                return null;
            }

            // Try to get email from introspection response
            String email = (String) json.get("email");
            if (email != null && !email.isBlank()) {
                return email;
            }

            email = (String) json.get("preferred_username");
            if (email != null && !email.isBlank()) {
                return email;
            }

            email = (String) json.get("username");
            if (email != null && !email.isBlank()) {
                return email;
            }

            // Log all available keys for debugging
            Log.debugf("Introspection response keys: %s", json.keySet());
            return null;
        } catch (Exception e) {
            Log.debugf("Error in introspection: %s", e.getMessage());
            return null;
        } finally {
            client.close();
        }
    }

    /**
     * Validates if token is active.
     */
    public boolean isTokenValid(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        Client client = ClientBuilder.newClient();
        try {
            String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Form form = new Form().param("token", accessToken);

            Response res = client.target(getIntrospectUrl())
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Basic " + basic)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

            String body = res.readEntity(String.class);
            if (res.getStatus() != 200) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(body, Map.class);
            return Boolean.TRUE.equals(json.get("active"));
        } catch (Exception e) {
            Log.debugf("Error validating token: %s", e.getMessage());
            return false;
        } finally {
            client.close();
        }
    }

    /**
     * Extracts roles from JWT token.
     * Keycloak stores roles in the 'realm_access' claim with structure:
     * {"realm_access": {"roles": ["user", ...]}}
     * or in 'resource_access' for client-specific roles.
     * 
     * @param accessToken The JWT access token
     * @return List of role names, or empty list if no roles found
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> getUserRolesFromToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return java.util.Collections.emptyList();
        }

        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                return java.util.Collections.emptyList();
            }

            // Decode payload
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

            java.util.List<String> roles = new java.util.ArrayList<>();

            // Try to get realm roles (most common in Keycloak)
            Object realmAccessObj = claims.get("realm_access");
            if (realmAccessObj instanceof Map) {
                Map<String, Object> realmAccess = (Map<String, Object>) realmAccessObj;
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof java.util.List) {
                    java.util.List<?> rolesList = (java.util.List<?>) rolesObj;
                    for (Object role : rolesList) {
                        if (role instanceof String) {
                            roles.add((String) role);
                        }
                    }
                }
            }

            // Also check resource_access for client-specific roles
            Object resourceAccessObj = claims.get("resource_access");
            if (resourceAccessObj instanceof Map) {
                Map<String, Object> resourceAccess = (Map<String, Object>) resourceAccessObj;
                // Check for roles in the client
                Object clientObj = resourceAccess.get(clientId);
                if (clientObj instanceof Map) {
                    Map<String, Object> client = (Map<String, Object>) clientObj;
                    Object clientRolesObj = client.get("roles");
                    if (clientRolesObj instanceof java.util.List) {
                        java.util.List<?> clientRolesList = (java.util.List<?>) clientRolesObj;
                        for (Object role : clientRolesList) {
                            if (role instanceof String && !roles.contains(role)) {
                                roles.add((String) role);
                            }
                        }
                    }
                }
            }

            Log.debugf("Extracted roles from token: %s", roles);
            return roles;
        } catch (Exception e) {
            Log.debugf("Error extracting roles from token: %s", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Checks if the user has a specific role.
     * 
     * @param accessToken The JWT access token
     * @param requiredRole The role to check for
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String accessToken, String requiredRole) {
        java.util.List<String> roles = getUserRolesFromToken(accessToken);
        boolean hasRole = roles.contains(requiredRole);
        Log.debugf("User has role '%s': %s (available roles: %s)", requiredRole, hasRole, roles);
        return hasRole;
    }
}

