package cloudSecurity.service;

import cloudSecurity.entity.Dish;
import cloudSecurity.entity.Category;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.dto.DishDTO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for dish business logic.
 */
@ApplicationScoped
public class DishService {

    /**
     * Finds all dishes for a restaurant, optionally filtered by category and availability.
     * Excludes soft-deleted dishes (deleted_at IS NULL).
     */
    public List<Dish> findAllByRestaurantId(UUID restaurantId, UUID categoryId, Boolean available) {
        String query = "restaurant.id = ?1 and deletedAt is null";
        Object[] params = new Object[]{restaurantId};
        
        if (categoryId != null && available != null) {
            query += " and category.id = ?2 and available = ?3";
            params = new Object[]{restaurantId, categoryId, available};
        } else if (categoryId != null) {
            query += " and category.id = ?2";
            params = new Object[]{restaurantId, categoryId};
        } else if (available != null) {
            query += " and available = ?2";
            params = new Object[]{restaurantId, available};
        }
        
        query += " order by category.position, position";
        return Dish.find(query, params).list();
    }

    /**
     * Finds a dish by ID and restaurant ID. Returns null if not found or doesn't belong to restaurant.
     * Excludes soft-deleted dishes.
     */
    public Dish findByIdAndRestaurantId(UUID dishId, UUID restaurantId) {
        return Dish.find("id = ?1 and restaurant.id = ?2 and deletedAt is null", dishId, restaurantId).firstResult();
    }

    /**
     * Finds a dish by ID and restaurant ID or throws NotFoundException.
     */
    public Dish findByIdAndRestaurantIdOrThrow(UUID dishId, UUID restaurantId) {
        Dish dish = findByIdAndRestaurantId(dishId, restaurantId);
        if (dish == null) {
            throw new NotFoundException("Dish not found or doesn't belong to restaurant");
        }
        return dish;
    }

    /**
     * Creates a new dish for the given restaurant and category.
     */
    @Transactional
    public Dish create(UUID restaurantId, DishDTO.CreateDishRequest request) {
        Restaurant restaurant = Restaurant.findById(restaurantId);
        if (restaurant == null) {
            throw new NotFoundException("Restaurant not found");
        }

        Category category = Category.findById(request.categoryId());
        if (category == null || !category.restaurant.id.equals(restaurantId)) {
            throw new NotFoundException("Category not found or doesn't belong to restaurant");
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.name().length() > 100) {
            throw new IllegalArgumentException("Name must be at most 100 characters");
        }

        if (request.description() != null && request.description().length() > 300) {
            throw new IllegalArgumentException("Description must be at most 300 characters");
        }

        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price is required and must be non-negative");
        }

        if (request.offerPrice() != null && request.offerPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Offer price must be non-negative");
        }

        Integer position = request.position();
        if (position == null) {
            List<Dish> existing = Dish.find("category.id = ?1 and deletedAt is null order by position", request.categoryId()).list();
            position = existing.isEmpty() ? 1 : existing.stream()
                    .mapToInt(d -> d.position)
                    .max()
                    .orElse(0) + 1;
        }

        Dish dish = new Dish(restaurant, category, request.name().trim(), request.price(), position);
        dish.description = request.description() != null ? request.description().trim() : null;
        dish.offerPrice = request.offerPrice();
        dish.imageUrl = request.imageUrl();
        dish.available = true;
        dish.featured = request.featured() != null ? request.featured() : false;
        dish.tags = request.tags();

        dish.persist();
        return dish;
    }

    /**
     * Updates an existing dish.
     */
    @Transactional
    public Dish update(UUID dishId, UUID restaurantId, DishDTO.UpdateDishRequest request) {
        Dish dish = findByIdAndRestaurantIdOrThrow(dishId, restaurantId);

        if (request.categoryId() != null) {
            Category category = Category.findById(request.categoryId());
            if (category == null || !category.restaurant.id.equals(restaurantId)) {
                throw new NotFoundException("Category not found or doesn't belong to restaurant");
            }
            dish.category = category;
        }

        if (request.name() != null && !request.name().isBlank()) {
            if (request.name().length() > 100) {
                throw new IllegalArgumentException("Name must be at most 100 characters");
            }
            dish.name = request.name().trim();
        }

        if (request.description() != null) {
            if (request.description().length() > 300) {
                throw new IllegalArgumentException("Description must be at most 300 characters");
            }
            dish.description = request.description().trim();
        }

        if (request.price() != null) {
            if (request.price().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price must be non-negative");
            }
            dish.price = request.price();
        }

        if (request.offerPrice() != null) {
            if (request.offerPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Offer price must be non-negative");
            }
            dish.offerPrice = request.offerPrice();
        }

        if (request.imageUrl() != null) {
            dish.imageUrl = request.imageUrl();
        }
        if (request.position() != null) {
            dish.position = request.position();
        }
        if (request.available() != null) {
            dish.available = request.available();
        }
        if (request.featured() != null) {
            dish.featured = request.featured();
        }
        if (request.tags() != null) {
            dish.tags = request.tags();
        }

        return dish;
    }

    /**
     * Soft deletes a dish.
     */
    @Transactional
    public void delete(UUID dishId, UUID restaurantId) {
        Dish dish = findByIdAndRestaurantIdOrThrow(dishId, restaurantId);
        dish.delete(); // Soft delete via @SoftDelete annotation
    }

    /**
     * Toggles dish availability.
     */
    @Transactional
    public Dish toggleAvailability(UUID dishId, UUID restaurantId) {
        Dish dish = findByIdAndRestaurantIdOrThrow(dishId, restaurantId);
        dish.available = !dish.available;
        return dish;
    }
}

