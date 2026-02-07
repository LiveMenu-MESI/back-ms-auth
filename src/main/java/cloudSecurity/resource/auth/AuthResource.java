package cloudSecurity.resource.auth;

import cloudSecurity.service.auth.KeycloakAdminService;
import cloudSecurity.service.auth.KeycloakIntrospectionService;
import cloudSecurity.service.auth.SessionService;

import java.util.Map;

import io.quarkus.oidc.client.OidcClient;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for authentication: register, login, logout.
 * Exposes /api/v1/auth/*. Accepts and returns JSON; supports cookie (SESSION) and Authorization: Bearer.
 */
@Path("/api/v1/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    OidcClient oidcClient;
    @Inject
    SessionService sessionService;
    @Inject
    KeycloakAdminService keycloakAdmin;
    @Inject
    KeycloakIntrospectionService keycloakIntrospection;

    /** Creates a new user in the identity provider. Returns 201 on success, 400 on validation/duplicate error. */
    @POST
    @Path("/register")
    public Response register(RegisterRequest req) {
        // Validate input
        if (req.email() == null || req.email().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Email is required"))
                    .build();
        }
        
        // Validate email format
        String email = req.email().trim().toLowerCase();
        if (!isValidEmail(email)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid email format"))
                    .build();
        }
        
        if (req.password() == null || req.password().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Password is required"))
                    .build();
        }
        if (req.password().length() < 8) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Password must be at least 8 characters"))
                    .build();
        }

        try {
            keycloakAdmin.createUser(email, req.password());
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("message", "User registered successfully"))
                    .build();
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Registration failed";
            // Clean up error message - remove JSON wrapping if present
            if (msg.contains("\"error\"")) {
                // Try to extract just the error message
                int start = msg.indexOf("\"error\"");
                if (start >= 0) {
                    int valueStart = msg.indexOf(":", start);
                    if (valueStart >= 0) {
                        int quoteStart = msg.indexOf("\"", valueStart);
                        if (quoteStart >= 0) {
                            int quoteEnd = msg.indexOf("\"", quoteStart + 1);
                            if (quoteEnd >= 0) {
                                msg = msg.substring(quoteStart + 1, quoteEnd);
                            }
                        }
                    }
                }
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", msg))
                    .build();
        }
    }

    /** Exchanges email/password for tokens. Returns access_token, refresh_token, expires_in in JSON; sets SESSION and REFRESH_TOKEN cookies for logout. */
    @POST
    @Path("/login")
    public Uni<Response> login(LoginRequest req) {
        return oidcClient.getTokens(Map.of(
                        "username", req.email(),
                        "password", req.password()))
                .onItem().transform(tokens -> {
                    String accessToken = tokens.getAccessToken();
                    String refreshToken = tokens.getRefreshToken();
                    int expiresIn = 3600;

                    Map<String, Object> body = Map.of(
                            "access_token", accessToken,
                            "refresh_token", refreshToken != null ? refreshToken : "",
                            "expires_in", expiresIn,
                            "token_type", "Bearer"
                    );

                    NewCookie sessionCookie = new NewCookie(
                            "SESSION",
                            accessToken,
                            "/",
                            null,
                            "auth",
                            expiresIn,
                            true,
                            true);
                    int refreshMaxAge = 7 * 24 * 3600; // 7 days for refresh token cookie
                    NewCookie refreshCookie = new NewCookie(
                            "REFRESH_TOKEN",
                            refreshToken != null ? refreshToken : "",
                            "/",
                            null,
                            "auth",
                            refreshMaxAge,
                            true,
                            true);

                    return Response.ok(body).cookie(sessionCookie).cookie(refreshCookie).build();
                });
       
    }

    /** Revokes access and refresh tokens in Keycloak and clears SESSION and REFRESH_TOKEN cookies. */
    @POST
    @Path("/logout")
    public Response logout(
            @CookieParam("SESSION") Cookie sessionCookie,
            @CookieParam("REFRESH_TOKEN") Cookie refreshCookie) {
        String accessToken = sessionCookie != null ? sessionCookie.getValue() : null;
        String refreshToken = refreshCookie != null ? refreshCookie.getValue() : null;
        sessionService.logoutFromKeycloak(accessToken, refreshToken);

        NewCookie clearSession = new NewCookie("SESSION", "", "/", null, "auth", 0, true, true);
        NewCookie clearRefresh = new NewCookie("REFRESH_TOKEN", "", "/", null, "auth", 0, true, true);
        return Response.noContent().cookie(clearSession).cookie(clearRefresh).build();
    }

    /** Protected endpoint: requires valid JWT. Validates token with Keycloak introspection; returns 200 if active, 401 if invalid or revoked. */
    @GET
    @Path("/user")
    @RolesAllowed("user")
    public Response currentUser(@HeaderParam("Authorization") String authorization) {
        String token = bearerToken(authorization);
        if (token == null || !keycloakIntrospection.introspect(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok("OK").build();
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    /**
     * Validates email format using a simple regex pattern.
     */
    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        // Simple email validation pattern
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }
}

record LoginRequest(String email, String password) {}
record RegisterRequest(String email, String password) {}

