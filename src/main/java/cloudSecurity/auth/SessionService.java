package cloudSecurity.auth;

import io.quarkus.oidc.client.OidcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SessionService {

    @Inject
    OidcClient oidcClient;

    public void logoutFromKeycloak(String refreshToken) {
        oidcClient.revokeAccessToken(refreshToken).await().indefinitely();
    }
}
