package cloudSecurity.resource;

import cloudSecurity.dto.DishDTO;
import cloudSecurity.dto.ImageDTO;
import cloudSecurity.service.DishService;
import cloudSecurity.service.ImageService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkus.logging.Log;

/**
 * REST resource for dish management (CU-04).
 * Endpoints under /api/v1/admin/restaurants/{restaurantId}/dishes require authentication.
 */
@Path("/api/v1/admin/restaurants/{restaurantId}/dishes")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class DishResource extends BaseResource {

    @Inject
    DishService dishService;

    @Inject
    ImageService imageService;

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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            var dish = dishService.findByIdAndRestaurantIdOrThrow(dishId, restaurantId);
            return Response.ok(DishDTO.DishResponse.from(dish)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Dish not found");
        }
    }

    /**
     * Creates a new dish for the restaurant (JSON).
     * POST /api/v1/admin/restaurants/{restaurantId}/dishes
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createDish(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            DishDTO.CreateDishRequest request) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            if (request == null) {
                return badRequest("Request body is required");
            }

            var dish = dishService.create(restaurantId, request);
            return Response.status(Response.Status.CREATED)
                    .entity(DishDTO.DishResponse.from(dish))
                    .build();
        } catch (IllegalArgumentException | jakarta.ws.rs.NotFoundException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            Log.errorf("Error creating dish: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to create dish: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Creates a new dish for the restaurant with image upload (multipart/form-data).
     * POST /api/v1/admin/restaurants/{restaurantId}/dishes/with-image
     * 
     * The image field can be included to upload and process the image automatically.
     */
    @POST
    @Path("/with-image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createDishWithImage(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @FormParam("categoryId") String categoryIdStr,
            @FormParam("name") String name,
            @FormParam("description") String description,
            @FormParam("price") String priceStr,
            @FormParam("offerPrice") String offerPriceStr,
            @FormParam("imageUrl") String imageUrl,
            @FormParam("featured") String featuredStr,
            @FormParam("tags") String tagsStr,
            @FormParam("position") String positionStr,
            @FormParam("image") FileUpload imageFile) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            DishDTO.CreateDishRequest request = buildCreateRequestFromMultipart(
                    categoryIdStr, name, description, priceStr, offerPriceStr,
                    imageUrl, featuredStr, tagsStr, positionStr, imageFile);

            var dish = dishService.create(restaurantId, request);
            return Response.status(Response.Status.CREATED)
                    .entity(DishDTO.DishResponse.from(dish))
                    .build();
        } catch (IllegalArgumentException | jakarta.ws.rs.NotFoundException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            Log.errorf("Error creating dish: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to create dish: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Updates a specific dish by ID (JSON).
     * PUT /api/v1/admin/restaurants/{restaurantId}/dishes/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDish(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID dishId,
            DishDTO.UpdateDishRequest request) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            if (request == null) {
                return badRequest("Request body is required");
            }

            var dish = dishService.update(dishId, restaurantId, request);
            return Response.ok(DishDTO.DishResponse.from(dish)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Dish not found");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            Log.errorf("Error updating dish: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to update dish: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Updates a specific dish by ID with image upload (multipart/form-data).
     * PUT /api/v1/admin/restaurants/{restaurantId}/dishes/{id}/with-image
     * 
     * If an image is provided, it will be automatically processed and uploaded to GCP Storage.
     */
    @PUT
    @Path("/{id}/with-image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateDishWithImage(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @PathParam("id") UUID dishId,
            @FormParam("categoryId") String categoryIdStr,
            @FormParam("name") String name,
            @FormParam("description") String description,
            @FormParam("price") String priceStr,
            @FormParam("offerPrice") String offerPriceStr,
            @FormParam("imageUrl") String imageUrl,
            @FormParam("available") String availableStr,
            @FormParam("featured") String featuredStr,
            @FormParam("tags") String tagsStr,
            @FormParam("position") String positionStr,
            @FormParam("image") FileUpload imageFile) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            DishDTO.UpdateDishRequest request = buildUpdateRequestFromMultipart(
                    categoryIdStr, name, description, priceStr, offerPriceStr,
                    imageUrl, availableStr, featuredStr, tagsStr, positionStr, imageFile);

            var dish = dishService.update(dishId, restaurantId, request);
            return Response.ok(DishDTO.DishResponse.from(dish)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Dish not found");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            Log.errorf("Error updating dish: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to update dish: " + e.getMessage()))
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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            dishService.delete(dishId, restaurantId);
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Dish not found");
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
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            var dish = dishService.toggleAvailability(dishId, restaurantId);
            return Response.ok(DishDTO.DishResponse.from(dish)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Dish not found");
        }
    }

    /**
     * Builds a CreateDishRequest from multipart form data.
     * If an image file is provided, it processes and uploads it automatically.
     */
    private DishDTO.CreateDishRequest buildCreateRequestFromMultipart(
            String categoryIdStr,
            String name,
            String description,
            String priceStr,
            String offerPriceStr,
            String imageUrl,
            String featuredStr,
            String tagsStr,
            String positionStr,
            FileUpload imageFile) throws Exception {
        
        // Parse categoryId
        UUID categoryId = null;
        if (categoryIdStr != null && !categoryIdStr.isBlank()) {
            try {
                categoryId = UUID.fromString(categoryIdStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid categoryId format");
            }
        } else {
            throw new IllegalArgumentException("categoryId is required");
        }

        // Parse price
        BigDecimal price = null;
        if (priceStr != null && !priceStr.isBlank()) {
            try {
                price = new BigDecimal(priceStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid price format");
            }
        } else {
            throw new IllegalArgumentException("price is required");
        }

        // Parse offerPrice (optional)
        BigDecimal offerPrice = null;
        if (offerPriceStr != null && !offerPriceStr.isBlank()) {
            try {
                offerPrice = new BigDecimal(offerPriceStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid offerPrice format");
            }
        }

        // Parse featured (optional)
        Boolean featured = null;
        if (featuredStr != null && !featuredStr.isBlank()) {
            featured = Boolean.parseBoolean(featuredStr);
        }

        // Parse position (optional)
        Integer position = null;
        if (positionStr != null && !positionStr.isBlank()) {
            try {
                position = Integer.parseInt(positionStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid position format");
            }
        }

        // Parse tags (optional, comma-separated)
        List<String> tags = null;
        if (tagsStr != null && !tagsStr.isBlank()) {
            tags = List.of(tagsStr.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        // Process image if provided
        String finalImageUrl = imageUrl;
        if (imageFile != null) {
            try {
                String contentType = imageFile.contentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("File must be an image (JPEG, PNG, or WebP)");
                }

                try (InputStream inputStream = java.nio.file.Files.newInputStream(imageFile.uploadedFile())) {
                    ImageDTO.ImageUploadResponse imageResponse = imageService.processAndStoreImage(
                            inputStream,
                            contentType,
                            imageFile.fileName()
                    );
                    // Use the large variant URL as the main image URL
                    finalImageUrl = imageResponse.largeUrl();
                }
            } catch (Exception e) {
                Log.errorf("Error processing image: %s", e.getMessage());
                throw new IllegalArgumentException("Failed to process image: " + e.getMessage());
            }
        }

        return new DishDTO.CreateDishRequest(
                categoryId,
                name,
                description,
                price,
                offerPrice,
                finalImageUrl,
                featured,
                tags,
                position
        );
    }

    /**
     * Builds an UpdateDishRequest from multipart form data.
     * If an image file is provided, it processes and uploads it automatically.
     */
    private DishDTO.UpdateDishRequest buildUpdateRequestFromMultipart(
            String categoryIdStr,
            String name,
            String description,
            String priceStr,
            String offerPriceStr,
            String imageUrl,
            String availableStr,
            String featuredStr,
            String tagsStr,
            String positionStr,
            FileUpload imageFile) throws Exception {
        
        // Parse categoryId (optional for update)
        UUID categoryId = null;
        if (categoryIdStr != null && !categoryIdStr.isBlank()) {
            try {
                categoryId = UUID.fromString(categoryIdStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid categoryId format");
            }
        }

        // Parse price (optional for update)
        BigDecimal price = null;
        if (priceStr != null && !priceStr.isBlank()) {
            try {
                price = new BigDecimal(priceStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid price format");
            }
        }

        // Parse offerPrice (optional)
        BigDecimal offerPrice = null;
        if (offerPriceStr != null && !offerPriceStr.isBlank()) {
            try {
                offerPrice = new BigDecimal(offerPriceStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid offerPrice format");
            }
        }

        // Parse available (optional)
        Boolean available = null;
        if (availableStr != null && !availableStr.isBlank()) {
            available = Boolean.parseBoolean(availableStr);
        }

        // Parse featured (optional)
        Boolean featured = null;
        if (featuredStr != null && !featuredStr.isBlank()) {
            featured = Boolean.parseBoolean(featuredStr);
        }

        // Parse position (optional)
        Integer position = null;
        if (positionStr != null && !positionStr.isBlank()) {
            try {
                position = Integer.parseInt(positionStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid position format");
            }
        }

        // Parse tags (optional, comma-separated)
        List<String> tags = null;
        if (tagsStr != null && !tagsStr.isBlank()) {
            tags = List.of(tagsStr.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        // Process image if provided
        String finalImageUrl = imageUrl;
        if (imageFile != null) {
            try {
                String contentType = imageFile.contentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("File must be an image (JPEG, PNG, or WebP)");
                }

                try (InputStream inputStream = java.nio.file.Files.newInputStream(imageFile.uploadedFile())) {
                    ImageDTO.ImageUploadResponse imageResponse = imageService.processAndStoreImage(
                            inputStream,
                            contentType,
                            imageFile.fileName()
                    );
                    // Use the large variant URL as the main image URL
                    finalImageUrl = imageResponse.largeUrl();
                }
            } catch (Exception e) {
                Log.errorf("Error processing image: %s", e.getMessage());
                throw new IllegalArgumentException("Failed to process image: " + e.getMessage());
            }
        }

        return new DishDTO.UpdateDishRequest(
                categoryId,
                name,
                description,
                price,
                offerPrice,
                finalImageUrl,
                available,
                featured,
                tags,
                position
        );
    }
}

