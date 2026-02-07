package cloudSecurity.resource;

import cloudSecurity.dto.CategoryDTO;
import cloudSecurity.service.CategoryService;
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
 * REST resource for category management (CU-03).
 * Endpoints under /api/v1/admin/restaurants/{restaurantId}/categories require authentication.
 */
@Path("/api/v1/admin/restaurants/{restaurantId}/categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class CategoryResource {

    @Inject
    CategoryService categoryService;

    @Inject
    RestaurantService restaurantService;

    @Inject
    TokenService tokenService;

    /**
     * Lists all categories for a restaurant, ordered by position.
     * GET /api/v1/admin/restaurants/{restaurantId}/categories
     */
    @GET
    public Response listCategories(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId) {
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

        List<CategoryDTO.CategoryResponse> categories = categoryService.findAllByRestaurantId(restaurantId)
                .stream()
                .map(CategoryDTO.CategoryResponse::from)
                .collect(Collectors.toList());
        return Response.ok(categories).build();
    }

    /**
     * Gets a specific category by ID.
     * GET /api/v1/admin/restaurants/{restaurantId}/categories/{id}
     */
    @GET
    @Path("/{id}")
    public Response getCategory(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID categoryId) {
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
            var category = categoryService.findByIdAndRestaurantIdOrThrow(categoryId, restaurantId);
            return Response.ok(CategoryDTO.CategoryResponse.from(category)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Category not found"))
                    .build();
        }
    }

    /**
     * Creates a new category for the restaurant.
     * POST /api/v1/admin/restaurants/{restaurantId}/categories
     */
    @POST
    public Response createCategory(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            CategoryDTO.CreateCategoryRequest request) {
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
            var category = categoryService.create(restaurantId, request);
            return Response.status(Response.Status.CREATED)
                    .entity(CategoryDTO.CategoryResponse.from(category))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Updates a specific category by ID.
     * PUT /api/v1/admin/restaurants/{restaurantId}/categories/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateCategory(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID categoryId,
            CategoryDTO.UpdateCategoryRequest request) {
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
            var category = categoryService.update(categoryId, restaurantId, request);
            return Response.ok(CategoryDTO.CategoryResponse.from(category)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Category not found"))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Deletes a specific category by ID.
     * DELETE /api/v1/admin/restaurants/{restaurantId}/categories/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteCategory(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID categoryId) {
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
            categoryService.delete(categoryId, restaurantId);
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Category not found"))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Reorders categories by updating their positions.
     * PATCH /api/v1/admin/restaurants/{restaurantId}/categories/reorder
     */
    @PATCH
    @Path("/reorder")
    public Response reorderCategories(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            CategoryDTO.ReorderCategoriesRequest request) {
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
            categoryService.reorder(restaurantId, request);
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
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

