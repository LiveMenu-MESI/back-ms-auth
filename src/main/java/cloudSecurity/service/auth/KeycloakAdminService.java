package cloudSecurity.service.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public class KeycloakAdminService {

    @ConfigProperty(name = "keycloak.admin.url")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.admin.realm")
    String realm;

    @ConfigProperty(name = "keycloak.admin.client-id")
    String adminClientId;

    @ConfigProperty(name = "keycloak.admin.client-secret")
    String adminClientSecret;

    @ConfigProperty(name = "keycloak.admin.username", defaultValue = "")
    String adminUsername;

    @ConfigProperty(name = "keycloak.admin.password", defaultValue = "")
    String adminPassword;

    private volatile String adminToken;

    private String getTokenUrl(String realmName) {
        return keycloakUrl + "/realms/" + realmName + "/protocol/openid-connect/token";
    }

    private String getUsersUrl() {
        return keycloakUrl + "/admin/realms/" + realm + "/users";
    }

    @Inject
    ObjectMapper objectMapper;

    private static final int KEYCLOAK_CALL_TIMEOUT_SEC = 15;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private void obtainAdminToken() {
        Client client = ClientBuilder.newClient();
        try {
            Form form;
            String tokenRealm;
            if (adminUsername != null && !adminUsername.isBlank()) {
                tokenRealm = "master";
                form = new Form()
                        .param("grant_type", "password")
                        .param("client_id", "admin-cli")
                        .param("username", adminUsername)
                        .param("password", adminPassword);
            } else {
                tokenRealm = realm;
                form = new Form()
                        .param("grant_type", "client_credentials")
                        .param("client_id", adminClientId)
                        .param("client_secret", adminClientSecret);
            }

            Response res = client.target(getTokenUrl(tokenRealm))
                    .request(MediaType.APPLICATION_FORM_URLENCODED)
                    .post(Entity.form(form));

            String body = res.readEntity(String.class);
            if (res.getStatus() != 200) {
                throw new RuntimeException("Keycloak token error: " + res.getStatus() + " " + body);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(body, Map.class);
            adminToken = (String) json.get("access_token");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            client.close();
        }
    }

    public void createUser(String email, String password) {
        try {
            EXECUTOR.submit(() -> doCreateUser(email, password))
                    .get(KEYCLOAK_CALL_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Keycloak request timed out after " + KEYCLOAK_CALL_TIMEOUT_SEC + "s. Check Keycloak at " + keycloakUrl, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void doCreateUser(String email, String password) {
        if (adminToken == null) {
            synchronized (this) {
                if (adminToken == null) obtainAdminToken();
            }
        }

        Client client = ClientBuilder.newClient();
        try {
            ObjectNode user = objectMapper.createObjectNode()
                    .put("username", email)
                    .put("email", email)
                    .put("emailVerified", true)
                    .put("enabled", true);
            ArrayNode requiredActions = objectMapper.createArrayNode();
            user.set("requiredActions", requiredActions);
            ArrayNode credentials = objectMapper.createArrayNode();
            credentials.addObject()
                    .put("type", "password")
                    .put("value", password)
                    .put("temporary", false);
            user.set("credentials", credentials);

            String jsonBody = objectMapper.writeValueAsString(user);

            Response res = client.target(getUsersUrl())
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + adminToken)
                    .post(Entity.entity(jsonBody, MediaType.APPLICATION_JSON_TYPE));

            // If token expired (401), refresh and retry once
            if (res.getStatus() == 401) {
                synchronized (this) {
                    adminToken = null; // Clear expired token
                    obtainAdminToken(); // Get new token
                }
                // Retry with new token
                res.close();
                res = client.target(getUsersUrl())
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .post(Entity.entity(jsonBody, MediaType.APPLICATION_JSON_TYPE));
            }

            if (res.getStatus() == 409) {
                String errorBody = res.readEntity(String.class);
                throw new RuntimeException("Email already registered");
            }
            if (res.getStatus() >= 400) {
                String errorBody = res.readEntity(String.class);
                // Try to extract a meaningful error message
                String errorMessage = "Failed to create user";
                try {
                    if (errorBody != null && !errorBody.isBlank()) {
                        // Try to parse as JSON to get error message
                        Map<String, Object> errorJson = objectMapper.readValue(errorBody, Map.class);
                        Object errorObj = errorJson.get("error");
                        if (errorObj != null) {
                            errorMessage = errorObj.toString();
                        } else {
                            errorMessage = errorBody;
                        }
                    }
                } catch (Exception e) {
                    // If parsing fails, use the raw body or default message
                    errorMessage = errorBody != null && !errorBody.isBlank() ? errorBody : errorMessage;
                }
                throw new RuntimeException(errorMessage);
            }
            // Do not PUT the user after creation: sending user without credentials clears the password in Keycloak.
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }
}

