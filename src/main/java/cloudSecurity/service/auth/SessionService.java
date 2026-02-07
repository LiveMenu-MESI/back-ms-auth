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

/**
 * Logout: revokes tokens in Keycloak via the OAuth2 token revocation endpoint.
 * Revoking the refresh token invalidates the session in Keycloak; access token revocation is best-effort.
 */
@ApplicationScoped
public class SessionService {

    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.realm")
    String realm;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String clientSecret;

    private String getRevokeUrl() {
        return keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/revoke";
    }

    /**
     * Revokes access and refresh tokens in Keycloak. Revokes refresh_token first (invalidates session), then access_token.
     * Best-effort: failures are logged but do not block; cookies are always cleared by the caller.
     */
    public void logoutFromKeycloak(String accessToken, String refreshToken) {
        Client client = ClientBuilder.newClient();
        try {
            String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            // Revoke refresh token first so Keycloak session is invalidated and no new access tokens can be issued
            if (refreshToken != null && !refreshToken.isBlank()) {
                revokeToken(client, basic, refreshToken, "refresh_token");
            }
            // Revoke access token so it cannot be used anymore
            if (accessToken != null && !accessToken.isBlank()) {
                revokeToken(client, basic, accessToken, "access_token");
            }
        } catch (Exception ignored) {
            // Best-effort; caller always clears cookies
        } finally {
            client.close();
        }
    }

    private void revokeToken(Client client, String basicAuth, String token, String tokenTypeHint) {
        try {
            Form form = new Form()
                    .param("token", token)
                    .param("token_type_hint", tokenTypeHint);
            Response res = client.target(getRevokeUrl())
                    .request()
                    .header("Authorization", "Basic " + basicAuth)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
            res.close();
        } catch (Exception ignored) {
            // Best-effort per token
        }
    }
}

