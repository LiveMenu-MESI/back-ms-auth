package cloudSecurity.service.menu;

import cloudSecurity.dto.PublicMenuDTO;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.entity.Category;
import cloudSecurity.entity.Dish;
import cloudSecurity.service.restaurant.RestaurantService;
import cloudSecurity.service.restaurant.CategoryService;
import cloudSecurity.service.restaurant.DishService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;

import java.util.List;
import java.util.UUID;

/**
 * Service for public menu display (CU-06).
 * Handles caching and menu data retrieval for public access.
 */
@ApplicationScoped
public class PublicMenuService {

    @Inject
    RestaurantService restaurantService;

    @Inject
    CategoryService categoryService;

    @Inject
    DishService dishService;

    /**
     * Gets the public menu for a restaurant by slug.
     * Results are cached to improve performance.
     * 
     * @param slug Restaurant slug
     * @return Public menu response with restaurant info, categories, and dishes
     * @throws NotFoundException if restaurant not found or has no active dishes
     */
    @CacheResult(cacheName = "public-menu")
    public PublicMenuDTO.PublicMenuResponse getPublicMenuBySlug(String slug) {
        // Find restaurant by slug
        Restaurant restaurant = restaurantService.findBySlugOrThrow(slug);

        // Get active categories ordered by position
        List<Category> categories = Category.find(
                "restaurant.id = ?1 and active = true order by position",
                restaurant.id
        ).list();

        if (categories.isEmpty()) {
            throw new NotFoundException("Restaurant has no active categories");
        }

        // Get all available dishes for these categories in a single optimized query
        // This avoids N+1 queries by fetching all dishes in one call
        List<UUID> categoryIds = categories.stream().map(c -> c.id).toList();
        List<Dish> dishes = Dish.find(
                "restaurant.id = ?1 and category.id in ?2 and available = true and deletedAt is null order by position",
                restaurant.id,
                categoryIds
        ).list();

        if (dishes.isEmpty()) {
            throw new NotFoundException("Restaurant has no active dishes");
        }

        // Build and return response
        return PublicMenuDTO.PublicMenuResponse.from(restaurant, categories, dishes);
    }

    /**
     * Gets a single dish by restaurant slug and dish ID for public display.
     * Only returns dishes that are available and not soft-deleted.
     *
     * @param slug   Restaurant slug
     * @param dishId Dish UUID
     * @return Dish info for public display
     * @throws NotFoundException if restaurant or dish not found or dish not available
     */
    public PublicMenuDTO.DishInfo getDishBySlug(String slug, UUID dishId) {
        Restaurant restaurant = restaurantService.findBySlugOrThrow(slug);
        Dish dish = Dish.find(
                "id = ?1 and restaurant.id = ?2 and available = true and deletedAt is null",
                dishId,
                restaurant.id
        ).firstResult();
        if (dish == null) {
            throw new NotFoundException("Dish not found or not available");
        }
        return PublicMenuDTO.DishInfo.from(dish);
    }

    /**
     * Invalidates the cache for a restaurant's public menu.
     * Should be called when menu data changes (dishes or categories created/updated/deleted).
     */
    @CacheInvalidate(cacheName = "public-menu")
    public void invalidateMenuCache(String slug) {
        // No-op: invalidation is performed by the annotation using the slug as cache key
    }
}

