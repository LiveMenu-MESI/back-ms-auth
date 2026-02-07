package cloudSecurity.resource;

import cloudSecurity.service.RestaurantService;
import cloudSecurity.service.auth.TokenService;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.UUID;

/**
 * Base resource class with common functionality for authenticated resources.
 * Provides methods for token extraction, user email retrieval, and restaurant ownership validation.
 */
public abstract class BaseResource {

    @Inject
    protected TokenService tokenService;

    @Inject
    protected RestaurantService restaurantService;

    /**
     * Extracts and validates the JWT token, then returns the user email.
     * The token should already be validated by TokenValidationFilter.
     */
    protected String getCurrentUserEmail(String authorization) {
        String token = bearerToken(authorization);
        if (token == null) {
            return null;
        }
        // Token should already be validated by TokenValidationFilter
        return tokenService.getUserEmailFromToken(token);
    }

    /**
     * Extracts Bearer token from Authorization header.
     */
    public static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    /**
     * Validates that the restaurant belongs to the user.
     * Returns the user email if valid, or null if token is invalid.
     * Throws NotFoundException if restaurant doesn't exist or doesn't belong to user.
     */
    protected String validateRestaurantOwnership(String authorization, UUID restaurantId) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return null;
        }
        restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        return userEmail;
    }

    /**
     * Creates an UNAUTHORIZED response.
     */
    protected Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Invalid or missing token"))
                .build();
    }

    /**
     * Creates a NOT_FOUND response with a custom message.
     */
    protected Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", message))
                .build();
    }

    /**
     * Creates a BAD_REQUEST response with a custom message.
     */
    protected Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", message))
                .build();
    }
}

