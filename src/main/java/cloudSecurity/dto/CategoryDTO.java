package cloudSecurity.dto;

import cloudSecurity.entity.Category;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTOs for Category requests and responses.
 */
public class CategoryDTO {

    public record CreateCategoryRequest(
            String name,
            String description,
            Integer position
    ) {
    }

    public record UpdateCategoryRequest(
            String name,
            String description,
            Integer position,
            Boolean active
    ) {
    }

    public record ReorderCategoriesRequest(
            java.util.List<UUID> categoryIds
    ) {
    }

    public record CategoryResponse(
            UUID id,
            UUID restaurantId,
            String name,
            String description,
            Integer position,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.id,
                    category.restaurant.id,
                    category.name,
                    category.description,
                    category.position,
                    category.active,
                    category.createdAt,
                    category.updatedAt
            );
        }
    }
}

