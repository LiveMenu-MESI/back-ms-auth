package cloudSecurity.service.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.LinkedHashMap;
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

    @Inject
    KeycloakHttpClientProvider keycloakHttpClientProvider;

    private static final int KEYCLOAK_CALL_TIMEOUT_SEC = 15;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private void obtainAdminToken() {
        try {
            Map<String, String> formParams;
            String tokenRealm;
            if (adminUsername != null && !adminUsername.isBlank()) {
                tokenRealm = "master";
                formParams = new LinkedHashMap<>();
                formParams.put("grant_type", "password");
                formParams.put("client_id", "admin-cli");
                formParams.put("username", adminUsername);
                formParams.put("password", adminPassword);
            } else {
                tokenRealm = realm;
                formParams = new LinkedHashMap<>();
                formParams.put("grant_type", "client_credentials");
                formParams.put("client_id", adminClientId);
                formParams.put("client_secret", adminClientSecret);
            }

            KeycloakHttpClientProvider.KeycloakResponse res = keycloakHttpClientProvider.postForm(
                    getTokenUrl(tokenRealm), formParams, null);
            if (res.statusCode() != 200) {
                throw new RuntimeException("Keycloak token error: " + res.statusCode() + " " + res.body());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(res.body(), Map.class);
            adminToken = (String) json.get("access_token");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
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

            KeycloakHttpClientProvider.KeycloakResponse res = keycloakHttpClientProvider.postJson(
                    getUsersUrl(), jsonBody, adminToken);

            if (res.statusCode() == 401) {
                synchronized (this) {
                    adminToken = null;
                    obtainAdminToken();
                }
                res = keycloakHttpClientProvider.postJson(getUsersUrl(), jsonBody, adminToken);
            }

            if (res.statusCode() == 409) {
                throw new RuntimeException("Email already registered");
            }
            if (res.statusCode() >= 400) {
                String errorMessage = "Failed to create user";
                try {
                    if (res.body() != null && !res.body().isBlank()) {
                        Map<String, Object> errorJson = objectMapper.readValue(res.body(), Map.class);
                        Object errorObj = errorJson.get("error");
                        if (errorObj != null) {
                            errorMessage = errorObj.toString();
                        } else {
                            errorMessage = res.body();
                        }
                    }
                } catch (Exception e) {
                    errorMessage = res.body() != null && !res.body().isBlank() ? res.body() : errorMessage;
                }
                throw new RuntimeException(errorMessage);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }
}
