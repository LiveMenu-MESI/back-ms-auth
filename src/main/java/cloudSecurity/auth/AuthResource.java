package cloudSecurity.auth;

import java.util.Map;

import io.quarkus.oidc.client.OidcClient;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    OidcClient oidcClient;
    SessionService sessionService;

    @POST
    @Path("/login")
    public Uni<Response> login(LoginRequest req) {

        return oidcClient.getTokens(Map.of(
                "username", req.username(),
                "password", req.password())).onItem().transform(tokens -> {

                    NewCookie cookie = new NewCookie(
                            "SESSION",
                            tokens.getAccessToken(),
                            "/",
                            null,
                            "auth",
                            3600,
                            true,
                            true);

                    return Response.ok().cookie(cookie).build();
                });
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam("SESSION") Cookie sessionCookie) {

        if (sessionCookie != null) {
            sessionService.logoutFromKeycloak(sessionCookie.getValue());
        }

        NewCookie deleteCookie = new NewCookie(
                "SESSION",
                "",
                "/",
                null,
                "auth",
                0,
                true,
                true);

        return Response.noContent()
                .cookie(deleteCookie)
                .build();
    }

    @GET
    @Path("/user")
    @RolesAllowed("user")
    public String zonaUsuario() {
        return "Hola usuario";
    }
}

record LoginRequest(String username, String password) {
}
