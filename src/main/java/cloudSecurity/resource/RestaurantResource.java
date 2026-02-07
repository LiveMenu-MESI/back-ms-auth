package cloudSecurity.resource;

import cloudSecurity.entity.Restaurant;
import cloudSecurity.dto.RestaurantDTO;
import cloudSecurity.service.RestaurantService;
import cloudSecurity.service.auth.KeycloakIntrospectionService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.security.identity.SecurityIdentity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST resource for restaurant management (CU-02).
 * Endpoints under /api/v1/admin/restaurants require authentication.
 * Users can have multiple restaurants.
 */
@Path("/api/v1/admin/restaurants")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class RestaurantResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    RestaurantService restaurantService;

    @Inject
    KeycloakIntrospectionService keycloakIntrospection;

    /**
     * Lists all restaurants for the current user.
     * GET /api/v1/admin/restaurants
     */
    @GET
    public Response listRestaurants(@HeaderParam("Authorization") String authorization) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        List<Restaurant> restaurants = restaurantService.findAllByUserEmail(userEmail);
        List<RestaurantDTO.RestaurantResponse> responses = restaurants.stream()
                .map(RestaurantDTO.RestaurantResponse::from)
                .collect(Collectors.toList());
        return Response.ok(responses).build();
    }

    /**
     * Gets a specific restaurant by ID.
     * GET /api/v1/admin/restaurants/{id}
     */
    @GET
    @Path("/{id}")
    public Response getRestaurant(
            @HeaderParam("Authorization") String authorization,
            @PathParam("id") UUID restaurantId) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        try {
            Restaurant restaurant = restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
            return Response.ok(RestaurantDTO.RestaurantResponse.from(restaurant)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        }
    }

    /**
     * Creates a new restaurant for the current user.
     * POST /api/v1/admin/restaurants
     */
    @POST
    public Response createRestaurant(
            @HeaderParam("Authorization") String authorization,
            RestaurantDTO.CreateRestaurantRequest request) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        try {
            Restaurant restaurant = restaurantService.create(userEmail, request);
            return Response.status(Response.Status.CREATED)
                    .entity(RestaurantDTO.RestaurantResponse.from(restaurant))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Updates a specific restaurant by ID.
     * PUT /api/v1/admin/restaurants/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateRestaurant(
            @HeaderParam("Authorization") String authorization,
            @PathParam("id") UUID restaurantId,
            RestaurantDTO.UpdateRestaurantRequest request) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        try {
            Restaurant restaurant = restaurantService.update(restaurantId, userEmail, request);
            return Response.ok(RestaurantDTO.RestaurantResponse.from(restaurant)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Deletes a specific restaurant by ID.
     * DELETE /api/v1/admin/restaurants/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteRestaurant(
            @HeaderParam("Authorization") String authorization,
            @PathParam("id") UUID restaurantId) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        try {
            restaurantService.delete(restaurantId, userEmail);
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        }
    }

    /**
     * Extracts and validates the JWT token, then returns the user email.
     */
    private String getCurrentUserEmail(String authorization) {
        String token = bearerToken(authorization);
        if (token == null || !keycloakIntrospection.introspect(token)) {
            return null;
        }
        // Get user email from SecurityIdentity (set by Quarkus OIDC)
        return securityIdentity.getPrincipal().getName();
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}

