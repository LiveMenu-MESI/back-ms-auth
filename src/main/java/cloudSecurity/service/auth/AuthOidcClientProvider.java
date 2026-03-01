package cloudSecurity.service.auth;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant.Type;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provides the OIDC client used for login/refresh (password grant).
 * The client is created lazily on first use so the application can start
 * without requiring Keycloak to be reachable at startup (e.g. in Docker).
 */
@ApplicationScoped
public class AuthOidcClientProvider {

    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;
    @ConfigProperty(name = "keycloak.realm")
    String keycloakRealm;
    @ConfigProperty(name = "OIDC_CLIENT_ID", defaultValue = "livemenu-backend")
    String clientId;
    @ConfigProperty(name = "OIDC_CLIENT_SECRET")
    String clientSecret;

    @jakarta.inject.Inject
    OidcClients oidcClients;

    private volatile OidcClient cached;
    private volatile Uni<OidcClient> createUni;

    /**
     * Returns the auth OIDC client, creating it on first call (connects to Keycloak then).
     */
    public Uni<OidcClient> getAuthClient() {
        if (cached != null) {
            return Uni.createFrom().item(cached);
        }
        synchronized (this) {
            if (cached != null) {
                return Uni.createFrom().item(cached);
            }
            if (createUni == null) {
                String authServerUrl = keycloakUrl + "/realms/" + keycloakRealm;
                OidcClientConfig cfg = OidcClientConfig
                        .authServerUrl(authServerUrl)
                        .id("auth")
                        .clientId(clientId)
                        .credentials(clientSecret)
                        .grant(Type.PASSWORD)
                        .build();
                createUni = oidcClients.newClient(cfg)
                        .onItem().invoke(c -> cached = c);
            }
            return createUni;
        }
    }
}
