package cloudSecurity.service;

import cloudSecurity.entity.Category;
import cloudSecurity.entity.Dish;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.dto.CategoryDTO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Service for category business logic.
 */
@ApplicationScoped
public class CategoryService {

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

        // Validate name
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.name().length() > 50) {
            throw new IllegalArgumentException("Name must be at most 50 characters");
        }

        // Use provided position or calculate next position
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
        return category;
    }

    /**
     * Updates an existing category.
     */
    @Transactional
    public Category update(UUID categoryId, UUID restaurantId, CategoryDTO.UpdateCategoryRequest request) {
        Category category = findByIdAndRestaurantIdOrThrow(categoryId, restaurantId);

        // Update name if provided
        if (request.name() != null && !request.name().isBlank()) {
            if (request.name().length() > 50) {
                throw new IllegalArgumentException("Name must be at most 50 characters");
            }
            category.name = request.name().trim();
        }

        // Update other fields
        if (request.description() != null) {
            category.description = request.description().trim();
        }
        if (request.position() != null) {
            category.position = request.position();
        }
        if (request.active() != null) {
            category.active = request.active();
        }

        return category;
    }

    /**
     * Deletes a category. Only if it has no dishes associated.
     */
    @Transactional
    public void delete(UUID categoryId, UUID restaurantId) {
        Category category = findByIdAndRestaurantIdOrThrow(categoryId, restaurantId);

        // Check if category has dishes
        long dishCount = Dish.find("category.id = ?1", categoryId).count();
        if (dishCount > 0) {
            throw new IllegalArgumentException("Cannot delete category with associated dishes");
        }

        category.delete();
    }

    /**
     * Reorders categories by updating their positions based on the provided order.
     */
    @Transactional
    public void reorder(UUID restaurantId, CategoryDTO.ReorderCategoriesRequest request) {
        List<UUID> categoryIds = request.categoryIds();
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("Category IDs list cannot be empty");
        }

        // Verify all categories belong to the restaurant
        for (UUID categoryId : categoryIds) {
            Category category = findByIdAndRestaurantId(categoryId, restaurantId);
            if (category == null) {
                throw new NotFoundException("Category not found: " + categoryId);
            }
        }

        // Update positions
        for (int i = 0; i < categoryIds.size(); i++) {
            Category category = Category.findById(categoryIds.get(i));
            if (category != null) {
                category.position = i + 1;
            }
        }
    }
}

