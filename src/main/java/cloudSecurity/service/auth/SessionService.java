package cloudSecurity.service.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

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

    @ConfigProperty(name = "quarkus.oidc.credentials.secret", defaultValue = "")
    String clientSecret;

    @Inject
    KeycloakHttpClientProvider keycloakHttpClientProvider;

    private String getRevokeUrl() {
        return keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/revoke";
    }

    /**
     * Revokes access and refresh tokens in Keycloak. Revokes refresh_token first (invalidates session), then access_token.
     * Best-effort: failures are logged but do not block; cookies are always cleared by the caller.
     */
    public void logoutFromKeycloak(String accessToken, String refreshToken) {
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        if (refreshToken != null && !refreshToken.isBlank()) {
            revokeToken(basic, refreshToken, "refresh_token");
        }
        if (accessToken != null && !accessToken.isBlank()) {
            revokeToken(basic, accessToken, "access_token");
        }
    }

    private void revokeToken(String basicAuth, String token, String tokenTypeHint) {
        try {
            keycloakHttpClientProvider.postForm(getRevokeUrl(),
                    Map.of("token", token, "token_type_hint", tokenTypeHint),
                    basicAuth);
        } catch (Exception ignored) {
            // Best-effort per token
        }
    }
}
