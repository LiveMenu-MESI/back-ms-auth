package cloudSecurity.resource.auth;

import cloudSecurity.resource.BaseResource;
import cloudSecurity.service.auth.KeycloakAdminService;
import cloudSecurity.service.auth.KeycloakIntrospectionService;
import cloudSecurity.service.auth.SessionService;
import cloudSecurity.service.auth.TokenService;

import java.util.Map;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
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
    OidcClients oidcClients;
    @Inject
    SessionService sessionService;
    @Inject
    KeycloakAdminService keycloakAdmin;
    @Inject
    KeycloakIntrospectionService keycloakIntrospection;
    @Inject
    TokenService tokenService;

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
        OidcClient authClient = oidcClients.getClient("auth");
        return authClient.getTokens(Map.of(
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

    /** Refreshes access token using refresh token. Accepts refresh_token in body or REFRESH_TOKEN cookie. */
    @POST
    @Path("/refresh")
    public Uni<Response> refresh(RefreshTokenRequest req) {
        // Get refresh token from body or cookie
        String refreshToken = req != null && req.refresh_token() != null && !req.refresh_token().isBlank()
                ? req.refresh_token()
                : null;

        if (refreshToken == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Refresh token is required"))
                    .build());
        }

        OidcClient authClient = oidcClients.getClient("auth");
        return authClient.refreshTokens(refreshToken)
                .onItem().transform(tokens -> {
                    String newAccessToken = tokens.getAccessToken();
                    String newRefreshToken = tokens.getRefreshToken();
                    int expiresIn = 3600;

                    Map<String, Object> body = Map.of(
                            "access_token", newAccessToken,
                            "refresh_token", newRefreshToken != null ? newRefreshToken : refreshToken,
                            "expires_in", expiresIn,
                            "token_type", "Bearer"
                    );

                    NewCookie sessionCookie = new NewCookie(
                            "SESSION",
                            newAccessToken,
                            "/",
                            null,
                            "auth",
                            expiresIn,
                            true,
                            true);
                    int refreshMaxAge = 7 * 24 * 3600; // 7 days
                    NewCookie refreshCookie = new NewCookie(
                            "REFRESH_TOKEN",
                            newRefreshToken != null ? newRefreshToken : refreshToken,
                            "/",
                            null,
                            "auth",
                            refreshMaxAge,
                            true,
                            true);

                    return Response.ok(body).cookie(sessionCookie).cookie(refreshCookie).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    return Response.status(Response.Status.UNAUTHORIZED)
                            .entity(Map.of("error", "Invalid or expired refresh token"))
                            .build();
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

    /** Protected endpoint: returns current user (id, email) from JWT. Requires valid Bearer token; 401 if missing or invalid. */
    @GET
    @Path("/user")
    @RolesAllowed("user")
    public Response currentUser(@HeaderParam("Authorization") String authorization) {
        String token = bearerToken(authorization);
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Missing or invalid authorization header"))
                    .build();
        }
        TokenService.CurrentUser user = tokenService.getCurrentUserFromToken(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or expired token"))
                    .build();
        }
        return Response.ok(Map.of("id", user.id(), "email", user.email())).build();
    }

    // bearerToken method removed - use BaseResource.bearerToken() instead
    private static String bearerToken(String authorization) {
        return BaseResource.bearerToken(authorization);
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
record RefreshTokenRequest(String refresh_token) {}

