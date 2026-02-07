package cloudSecurity.dto;

import cloudSecurity.entity.Restaurant;
import cloudSecurity.entity.Category;
import cloudSecurity.entity.Dish;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTOs for public menu display (CU-06).
 */
public class PublicMenuDTO {

    /**
     * Complete public menu response.
     */
    public record PublicMenuResponse(
            RestaurantInfo restaurant,
            List<CategoryWithDishes> categories
    ) {
        public static PublicMenuResponse from(Restaurant restaurant, List<Category> categories, List<Dish> dishes) {
            // Group dishes by category
            Map<UUID, List<Dish>> dishesByCategory = dishes.stream()
                    .collect(Collectors.groupingBy(d -> d.category.id));

            List<CategoryWithDishes> categoriesWithDishes = categories.stream()
                    .map(category -> {
                        List<Dish> categoryDishes = dishesByCategory.getOrDefault(category.id, List.of());
                        return CategoryWithDishes.from(category, categoryDishes);
                    })
                    .collect(Collectors.toList());

            return new PublicMenuResponse(
                    RestaurantInfo.from(restaurant),
                    categoriesWithDishes
            );
        }
    }

    /**
     * Restaurant information for public display.
     */
    public record RestaurantInfo(
            UUID id,
            String name,
            String slug,
            String description,
            String logo,
            String phone,
            String address,
            Map<String, Object> schedule
    ) {
        public static RestaurantInfo from(Restaurant restaurant) {
            return new RestaurantInfo(
                    restaurant.id,
                    restaurant.name,
                    restaurant.slug,
                    restaurant.description,
                    restaurant.logo,
                    restaurant.phone,
                    restaurant.address,
                    restaurant.schedule
            );
        }
    }

    /**
     * Category with its dishes.
     */
    public record CategoryWithDishes(
            UUID id,
            String name,
            String description,
            Integer position,
            List<DishInfo> dishes
    ) {
        public static CategoryWithDishes from(Category category, List<Dish> dishes) {
            List<DishInfo> dishInfos = dishes.stream()
                    .map(DishInfo::from)
                    .collect(Collectors.toList());

            return new CategoryWithDishes(
                    category.id,
                    category.name,
                    category.description,
                    category.position,
                    dishInfos
            );
        }
    }

    /**
     * Dish information for public display.
     */
    public record DishInfo(
            UUID id,
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
        public static DishInfo from(Dish dish) {
            return new DishInfo(
                    dish.id,
                    dish.name,
                    dish.description,
                    dish.price,
                    dish.offerPrice,
                    dish.imageUrl,
                    dish.available,
                    dish.featured,
                    dish.tags,
                    dish.position
            );
        }
    }
}

