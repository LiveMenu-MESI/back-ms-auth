package cloudSecurity.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String clientSecret;

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
            return false;
        } finally {
            client.close();
        }
    }
}
