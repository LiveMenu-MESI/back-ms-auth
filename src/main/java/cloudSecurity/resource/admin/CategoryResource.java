package cloudSecurity.resource.admin;

import cloudSecurity.resource.BaseResource;
import cloudSecurity.dto.CategoryDTO;
import cloudSecurity.service.restaurant.CategoryService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
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
public class CategoryResource extends BaseResource {

    @Inject
    CategoryService categoryService;

    /**
     * Lists all categories for a restaurant, ordered by position.
     * GET /api/v1/admin/restaurants/{restaurantId}/categories
     */
    @GET
    public Response listCategories(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            var category = categoryService.findByIdAndRestaurantIdOrThrow(categoryId, restaurantId);
            return Response.ok(CategoryDTO.CategoryResponse.from(category)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Category not found");
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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            var category = categoryService.create(restaurantId, request);
            return Response.status(Response.Status.CREATED)
                    .entity(CategoryDTO.CategoryResponse.from(category))
                    .build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            var category = categoryService.update(categoryId, restaurantId, request);
            return Response.ok(CategoryDTO.CategoryResponse.from(category)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Category not found");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            categoryService.delete(categoryId, restaurantId);
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Category not found");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            categoryService.reorder(restaurantId, request);
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }
}

