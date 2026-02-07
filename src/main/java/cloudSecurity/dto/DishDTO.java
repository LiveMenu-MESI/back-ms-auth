package cloudSecurity.dto;

import cloudSecurity.entity.Dish;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for Dish requests and responses.
 */
public class DishDTO {

    public record CreateDishRequest(
            UUID categoryId,
            String name,
            String description,
            BigDecimal price,
            BigDecimal offerPrice,
            String imageUrl,
            Boolean featured,
            List<String> tags,
            Integer position
    ) {
    }

    public record UpdateDishRequest(
            UUID categoryId,
            String name,
            String description,
            BigDecimal price,
            BigDecimal offerPrice,
            String imageUrl,
            Boolean available,
            Boolean featured,
            List<String> tags,
            Integer position
    ) {
    }

    public record DishResponse(
            UUID id,
            UUID restaurantId,
            UUID categoryId,
            String name,
            String description,
            BigDecimal price,
            BigDecimal offerPrice,
            String imageUrl,
            Boolean available,
            Boolean featured,
            List<String> tags,
            Integer position,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static DishResponse from(Dish dish) {
            return new DishResponse(
                    dish.id,
                    dish.restaurant.id,
                    dish.category.id,
                    dish.name,
                    dish.description,
                    dish.price,
                    dish.offerPrice,
                    dish.imageUrl,
                    dish.available,
                    dish.featured,
                    dish.tags,
                    dish.position,
                    dish.createdAt,
                    dish.updatedAt
            );
        }
    }
}

