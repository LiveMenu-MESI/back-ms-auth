package cloudSecurity.dto;

import cloudSecurity.entity.Restaurant;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for Restaurant requests and responses.
 */
public class RestaurantDTO {

    public record CreateRestaurantRequest(
            String name,
            String description,
            String logo,
            String phone,
            String address,
            Map<String, Object> schedule
    ) {
    }

    public record UpdateRestaurantRequest(
            String name,
            String description,
            String logo,
            String phone,
            String address,
            Map<String, Object> schedule
    ) {
    }

    public record RestaurantResponse(
            UUID id,
            String name,
            String slug,
            String description,
            String logo,
            String phone,
            String address,
            Map<String, Object> schedule,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static RestaurantResponse from(Restaurant restaurant) {
            return new RestaurantResponse(
                    restaurant.id,
                    restaurant.name,
                    restaurant.slug,
                    restaurant.description,
                    restaurant.logo,
                    restaurant.phone,
                    restaurant.address,
                    restaurant.schedule,
                    restaurant.createdAt,
                    restaurant.updatedAt
            );
        }
    }
}

