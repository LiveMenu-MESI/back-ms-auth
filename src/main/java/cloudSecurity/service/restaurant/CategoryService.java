package cloudSecurity.service.restaurant;

import cloudSecurity.entity.Category;
import cloudSecurity.entity.Dish;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.dto.CategoryDTO;
import cloudSecurity.service.menu.PublicMenuService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Service for category business logic.
 */
@ApplicationScoped
public class CategoryService {

    @Inject
    PublicMenuService publicMenuService;

    /**
     * Finds all categories for a restaurant, ordered by position.
     */
    public List<Category> findAllByRestaurantId(UUID restaurantId) {
        return Category.find("restaurant.id = ?1 order by position", restaurantId).list();
    }

    /**
     * Finds a category by ID and restaurant ID. Returns null if not found or doesn't belong to restaurant.
     */
    public Category findByIdAndRestaurantId(UUID categoryId, UUID restaurantId) {
        return Category.find("id = ?1 and restaurant.id = ?2", categoryId, restaurantId).firstResult();
    }

    /**
     * Finds a category by ID and restaurant ID or throws NotFoundException.
     */
    public Category findByIdAndRestaurantIdOrThrow(UUID categoryId, UUID restaurantId) {
        Category category = findByIdAndRestaurantId(categoryId, restaurantId);
        if (category == null) {
            throw new NotFoundException("Category not found or doesn't belong to restaurant");
        }
        return category;
    }

    /**
     * Creates a new category for the given restaurant.
     */
    @Transactional
    public Category create(UUID restaurantId, CategoryDTO.CreateCategoryRequest request) {
        Restaurant restaurant = Restaurant.findById(restaurantId);
        if (restaurant == null) {
            throw new NotFoundException("Restaurant not found");
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.name().length() > 50) {
            throw new IllegalArgumentException("Name must be at most 50 characters");
        }

        Integer position = request.position();
        if (position == null) {
            List<Category> existing = findAllByRestaurantId(restaurantId);
            position = existing.isEmpty() ? 1 : existing.stream()
                    .mapToInt(c -> c.position)
                    .max()
                    .orElse(0) + 1;
        }

        Category category = new Category(restaurant, request.name().trim(), position);
        category.description = request.description() != null ? request.description().trim() : null;

        category.persist();
        publicMenuService.invalidateMenuCache(restaurant.slug);
        return category;
    }

    /**
     * Updates an existing category.
     */
    @Transactional
    public Category update(UUID categoryId, UUID restaurantId, CategoryDTO.UpdateCategoryRequest request) {
        Category category = findByIdAndRestaurantIdOrThrow(categoryId, restaurantId);

        if (request.name() != null && !request.name().isBlank()) {
            if (request.name().length() > 50) {
                throw new IllegalArgumentException("Name must be at most 50 characters");
            }
            category.name = request.name().trim();
        }

        if (request.description() != null) {
            category.description = request.description().trim();
        }
        if (request.position() != null) {
            category.position = request.position();
        }
        if (request.active() != null) {
            category.active = request.active();
        }

        invalidatePublicMenuCache(category.restaurant);
        return category;
    }

    /**
     * Deletes a category. Only if it has no dishes associated.
     */
    @Transactional
    public void delete(UUID categoryId, UUID restaurantId) {
        Category category = findByIdAndRestaurantIdOrThrow(categoryId, restaurantId);

        long dishCount = Dish.find("category.id = ?1", categoryId).count();
        if (dishCount > 0) {
            throw new IllegalArgumentException("Cannot delete category with associated dishes");
        }

        Restaurant restaurant = category.restaurant;
        category.delete();
        invalidatePublicMenuCache(restaurant);
    }

    /**
     * Reorders categories by updating their positions based on the provided order.
     * Optimized to fetch all categories in one query and update in batch.
     */
    @Transactional
    public void reorder(UUID restaurantId, CategoryDTO.ReorderCategoriesRequest request) {
        List<UUID> categoryIds = request.categoryIds();
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("Category IDs list cannot be empty");
        }

        List<Category> categories = Category.find("id in (?1) and restaurant.id = ?2", categoryIds, restaurantId).list();
        
        if (categories.size() != categoryIds.size()) {
            throw new NotFoundException("One or more categories not found or don't belong to restaurant");
        }

        java.util.Map<UUID, Category> categoryMap = categories.stream()
                .collect(java.util.stream.Collectors.toMap(c -> c.id, c -> c));

        for (int i = 0; i < categoryIds.size(); i++) {
            Category category = categoryMap.get(categoryIds.get(i));
            if (category != null) {
                category.position = i + 1;
            }
        }
        Restaurant restaurant = Restaurant.findById(restaurantId);
        if (restaurant != null) {
            invalidatePublicMenuCache(restaurant);
        }
    }

    private void invalidatePublicMenuCache(Restaurant restaurant) {
        if (restaurant != null && restaurant.slug != null) {
            publicMenuService.invalidateMenuCache(restaurant.slug);
        }
    }
}

