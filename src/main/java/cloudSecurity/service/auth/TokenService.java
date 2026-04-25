package cloudSecurity.service.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    /** Current user info returned by getCurrentUserFromToken. */
    public record CurrentUser(String id, String email) {}

    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.realm")
    String realm;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret", defaultValue = "")
    String clientSecret;

    @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
    boolean oidcEnabled;

    @Inject
    KeycloakHttpClientProvider keycloakHttpClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getIntrospectUrl() {
        return keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
    }

    /**
     * Returns the current user (id and email) from a valid access token.
     * Id is the Keycloak subject (sub). Returns null if token is invalid or user info cannot be extracted.
     */
    public CurrentUser getCurrentUserFromToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        CurrentUser fromJwt = extractCurrentUserFromJWT(accessToken);
        if (fromJwt != null && isTokenValid(accessToken)) {
            return fromJwt;
        }
        return extractCurrentUserFromIntrospection(accessToken);
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
     * Extracts current user (sub + email) from JWT payload. Does not validate token.
     */
    private CurrentUser extractCurrentUserFromJWT(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

            Object subObj = claims.get("sub");
            String id = subObj != null ? subObj.toString() : null;
            String email = (String) claims.get("email");
            if (email == null || email.isBlank()) {
                email = (String) claims.get("preferred_username");
            }
            if (email == null || email.isBlank()) {
                email = (String) claims.get("username");
            }
            if ((email == null || email.isBlank()) && id != null && id.contains("@")) {
                email = id;
            }
            if (id == null || id.isBlank() || email == null || email.isBlank()) {
                return null;
            }
            return new CurrentUser(id, email);
        } catch (Exception e) {
            Log.debugf("Error extracting current user from JWT: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts email from JWT token by decoding the payload.
     * JWT format: header.payload.signature
     */
    private String extractEmailFromJWT(String token) {
        CurrentUser user = extractCurrentUserFromJWT(token);
        return user != null ? user.email() : null;
    }

    /**
     * Extracts current user (sub + email) from token introspection response.
     * Returns null if token is not active or user info cannot be extracted.
     */
    private CurrentUser extractCurrentUserFromIntrospection(String accessToken) {
        try {
            String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            KeycloakHttpClientProvider.KeycloakResponse res = keycloakHttpClientProvider.postForm(
                    getIntrospectUrl(), Map.of("token", accessToken), basic);
            if (res.statusCode() != 200) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(res.body(), Map.class);
            if (!Boolean.TRUE.equals(json.get("active"))) {
                return null;
            }
            Object subObj = json.get("sub");
            String id = subObj != null ? subObj.toString() : null;
            String email = (String) json.get("email");
            if (email == null || email.isBlank()) {
                email = (String) json.get("preferred_username");
            }
            if (email == null || email.isBlank()) {
                email = (String) json.get("username");
            }
            if ((email == null || email.isBlank()) && id != null && id.contains("@")) {
                email = id;
            }
            if (id == null || id.isBlank() || email == null || email.isBlank()) {
                return null;
            }
            return new CurrentUser(id, email);
        } catch (Exception e) {
            Log.debugf("Error in introspection: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts email from token introspection response.
     * Note: This method assumes the token has already been validated.
     * It does NOT validate the token again.
     */
    private String extractEmailFromIntrospection(String accessToken) {
        CurrentUser user = extractCurrentUserFromIntrospection(accessToken);
        return user != null ? user.email() : null;
    }

    /**
     * Validates if token is active.
     */
    public boolean isTokenValid(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        if (!oidcEnabled) {
            // OIDC disabled (test profile): skip HTTP validation, trust JWT structure
            return true;
        }
        try {
            String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            KeycloakHttpClientProvider.KeycloakResponse res = keycloakHttpClientProvider.postForm(
                    getIntrospectUrl(), Map.of("token", accessToken), basic);
            if (res.statusCode() != 200) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(res.body(), Map.class);
            return Boolean.TRUE.equals(json.get("active"));
        } catch (Exception e) {
            Log.debugf("Error validating token: %s", e.getMessage());
            return false;
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

