package cloudSecurity.service.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates an access token against Keycloak's token introspection endpoint.
 * Returns true only if Keycloak reports the token as active (not revoked, not expired).
 */
@ApplicationScoped
public class KeycloakIntrospectionService {

    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.realm")
    String realm;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret", defaultValue = "")
    String clientSecret;

    @Inject
    KeycloakHttpClientProvider keycloakHttpClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getIntrospectUrl() {
        return keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
    }

    /**
     * Calls Keycloak token introspection endpoint. Returns true if token is active, false otherwise.
     */
    public boolean introspect(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        try {
            String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            KeycloakHttpClientProvider.KeycloakResponse res = keycloakHttpClientProvider.postForm(
                    getIntrospectUrl(),
                    Map.of("token", accessToken),
                    basic);
            if (res.statusCode() != 200) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(res.body(), Map.class);
            return Boolean.TRUE.equals(json.get("active"));
        } catch (Exception e) {
            return false;
        }
    }
}
