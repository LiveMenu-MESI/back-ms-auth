package cloudSecurity.resource;

import cloudSecurity.dto.DishDTO;
import cloudSecurity.service.DishService;
import cloudSecurity.service.RestaurantService;
import cloudSecurity.service.auth.TokenService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST resource for dish management (CU-04).
 * Endpoints under /api/v1/admin/restaurants/{restaurantId}/dishes require authentication.
 */
@Path("/api/v1/admin/restaurants/{restaurantId}/dishes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class DishResource {

    @Inject
    DishService dishService;

    @Inject
    RestaurantService restaurantService;

    @Inject
    TokenService tokenService;

    /**
     * Lists all dishes for a restaurant, with optional filters.
     * GET /api/v1/admin/restaurants/{restaurantId}/dishes?categoryId=...&available=...
     */
    @GET
    public Response listDishes(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @QueryParam("categoryId") UUID categoryId,
            @QueryParam("available") Boolean available) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        // Verify restaurant belongs to user
        try {
            restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        }

        List<DishDTO.DishResponse> dishes = dishService.findAllByRestaurantId(restaurantId, categoryId, available)
                .stream()
                .map(DishDTO.DishResponse::from)
                .collect(Collectors.toList());
        return Response.ok(dishes).build();
    }

    /**
     * Gets a specific dish by ID.
     * GET /api/v1/admin/restaurants/{restaurantId}/dishes/{id}
     */
    @GET
    @Path("/{id}")
    public Response getDish(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID dishId) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        // Verify restaurant belongs to user
        try {
            restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        }

        try {
            var dish = dishService.findByIdAndRestaurantIdOrThrow(dishId, restaurantId);
            return Response.ok(DishDTO.DishResponse.from(dish)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Dish not found"))
                    .build();
        }
    }

    /**
     * Creates a new dish for the restaurant.
     * POST /api/v1/admin/restaurants/{restaurantId}/dishes
     */
    @POST
    public Response createDish(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            DishDTO.CreateDishRequest request) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        // Verify restaurant belongs to user
        try {
            restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        }

        try {
            var dish = dishService.create(restaurantId, request);
            return Response.status(Response.Status.CREATED)
                    .entity(DishDTO.DishResponse.from(dish))
                    .build();
        } catch (IllegalArgumentException | jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Updates a specific dish by ID.
     * PUT /api/v1/admin/restaurants/{restaurantId}/dishes/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateDish(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID dishId,
            DishDTO.UpdateDishRequest request) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        // Verify restaurant belongs to user
        try {
            restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        }

        try {
            var dish = dishService.update(dishId, restaurantId, request);
            return Response.ok(DishDTO.DishResponse.from(dish)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Dish not found"))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Soft deletes a specific dish by ID.
     * DELETE /api/v1/admin/restaurants/{restaurantId}/dishes/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteDish(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID dishId) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        // Verify restaurant belongs to user
        try {
            restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        }

        try {
            dishService.delete(dishId, restaurantId);
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Dish not found"))
                    .build();
        }
    }

    /**
     * Toggles dish availability.
     * PATCH /api/v1/admin/restaurants/{restaurantId}/dishes/{id}/availability
     */
    @PATCH
    @Path("/{id}/availability")
    public Response toggleAvailability(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID dishId) {
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid or missing token"))
                    .build();
        }

        // Verify restaurant belongs to user
        try {
            restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Restaurant not found"))
                    .build();
        }

        try {
            var dish = dishService.toggleAvailability(dishId, restaurantId);
            return Response.ok(DishDTO.DishResponse.from(dish)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Dish not found"))
                    .build();
        }
    }

    private String getCurrentUserEmail(String authorization) {
        String token = bearerToken(authorization);
        if (token == null) {
            return null;
        }
        // Get user email directly from token introspection
        return tokenService.getUserEmailFromToken(token);
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}

