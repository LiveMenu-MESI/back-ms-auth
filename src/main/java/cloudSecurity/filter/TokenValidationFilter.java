package cloudSecurity.filter;

import cloudSecurity.service.auth.TokenService;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

import io.quarkus.logging.Log;

/**
 * Filter that validates JWT tokens before allowing access to protected endpoints.
 * This ensures that invalid tokens are rejected with 401 Unauthorized.
 * 
 * Priority is set to AUTHENTICATION to run before authorization checks.
 * This filter only applies to /api/v1/admin/* endpoints.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TokenValidationFilter implements ContainerRequestFilter {

    @Inject
    TokenService tokenService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        
        // Only validate tokens for admin endpoints
        if (!path.startsWith("/api/v1/admin/")) {
            // Public endpoints, skip validation
            return;
        }

        // For protected admin endpoints, validate the token
        String authorization = requestContext.getHeaderString("Authorization");
        String token = extractBearerToken(authorization);

        if (token == null) {
            // No token provided
            Log.warnf("Unauthorized access attempt to %s: No token provided", path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Missing or invalid authorization header"))
                    .build()
            );
            return;
        }

        // Validate token - this will check if token is active and extract email
        String userEmail = tokenService.getUserEmailFromToken(token);
        if (userEmail == null) {
            // Token is invalid, expired, or email cannot be extracted
            Log.warnf("Unauthorized access attempt to %s: Invalid or expired token", path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or expired token"))
                    .build()
            );
            return;
        }

        // Authorization: Check if user has required role "user"
        if (!tokenService.hasRole(token, "user")) {
            Log.warnf("Forbidden access attempt to %s: User %s does not have required role 'user'", path, userEmail);
            requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions. Required role: user"))
                    .build()
            );
            return;
        }

        // Token is valid and user has required role, store user email in request context for later use
        requestContext.setProperty("userEmail", userEmail);
        Log.debugf("Token validated and authorized successfully for user: %s accessing %s", userEmail, path);
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}

