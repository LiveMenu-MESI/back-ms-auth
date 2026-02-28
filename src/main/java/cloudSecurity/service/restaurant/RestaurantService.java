package cloudSecurity.service.restaurant;

import cloudSecurity.entity.Restaurant;
import cloudSecurity.dto.RestaurantDTO;
import cloudSecurity.service.menu.PublicMenuService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.UUID;

/**
 * Service for restaurant business logic.
 */
@ApplicationScoped
public class RestaurantService {

    @Inject
    PublicMenuService publicMenuService;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");

    /**
     * Generates a URL-friendly slug from a restaurant name.
     * Example: "My Restaurant" -> "my-restaurant"
     */
    public String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = normalized.toLowerCase(Locale.ENGLISH);
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = EDGES_DASHES.matcher(slug).replaceAll("");

        // Ensure slug is not empty
        if (slug.isEmpty()) {
            slug = "restaurant-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // Make slug unique by appending a number if needed
        String baseSlug = slug;
        int counter = 1;
        while (Restaurant.find("slug", slug).firstResult() != null) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    /**
     * Finds all restaurants for a user.
     */
    public List<Restaurant> findAllByUserEmail(String userEmail) {
        return Restaurant.find("userEmail", userEmail).list();
    }

    /**
     * Finds a restaurant by ID and user email. Returns null if not found or doesn't belong to user.
     */
    public Restaurant findByIdAndUserEmail(UUID restaurantId, String userEmail) {
        return Restaurant.find("id = ?1 and userEmail = ?2", restaurantId, userEmail).firstResult();
    }

    /**
     * Finds a restaurant by ID and user email or throws NotFoundException.
     */
    public Restaurant findByIdAndUserEmailOrThrow(UUID restaurantId, String userEmail) {
        Restaurant restaurant = findByIdAndUserEmail(restaurantId, userEmail);
        if (restaurant == null) {
            throw new NotFoundException("Restaurant not found or doesn't belong to user: " + userEmail);
        }
        return restaurant;
    }

    /**
     * Creates a new restaurant for the given user.
     */
    @Transactional
    public Restaurant create(String userEmail, RestaurantDTO.CreateRestaurantRequest request) {
        // Validate name
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.name().length() > 100) {
            throw new IllegalArgumentException("Name must be at most 100 characters");
        }

        // Validate description
        if (request.description() != null && request.description().length() > 500) {
            throw new IllegalArgumentException("Description must be at most 500 characters");
        }

        Restaurant restaurant = new Restaurant(userEmail, request.name().trim());
        restaurant.slug = generateSlug(restaurant.name);
        restaurant.description = request.description() != null ? request.description().trim() : null;
        restaurant.logo = request.logo();
        restaurant.phone = request.phone();
        restaurant.address = request.address();
        restaurant.schedule = request.schedule();

        restaurant.persist();
        return restaurant;
    }

    /**
     * Updates an existing restaurant.
     */
    @Transactional
    public Restaurant update(UUID restaurantId, String userEmail, RestaurantDTO.UpdateRestaurantRequest request) {
        Restaurant restaurant = findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        String oldSlug = restaurant.slug;

        // Update name if provided
        if (request.name() != null && !request.name().isBlank()) {
            if (request.name().length() > 100) {
                throw new IllegalArgumentException("Name must be at most 100 characters");
            }
            restaurant.name = request.name().trim();
            // Regenerate slug if name changed
            restaurant.slug = generateSlug(restaurant.name);
        }

        // Update description
        if (request.description() != null) {
            if (request.description().length() > 500) {
                throw new IllegalArgumentException("Description must be at most 500 characters");
            }
            restaurant.description = request.description().trim();
        }

        // Update other fields
        restaurant.logo = request.logo();
        restaurant.phone = request.phone();
        restaurant.address = request.address();
        restaurant.schedule = request.schedule();

        // Invalidate public menu cache so name, schedule, logo, etc. are reflected
        publicMenuService.invalidateMenuCache(restaurant.slug);
        if (!restaurant.slug.equals(oldSlug)) {
            publicMenuService.invalidateMenuCache(oldSlug);
        }
        return restaurant;
    }

    /**
     * Deletes a restaurant for the given user.
     */
    @Transactional
    public void delete(UUID restaurantId, String userEmail) {
        Restaurant restaurant = findByIdAndUserEmailOrThrow(restaurantId, userEmail);
        String slug = restaurant.slug;
        restaurant.delete();
        publicMenuService.invalidateMenuCache(slug);
    }

    /**
     * Finds a restaurant by slug (for public access).
     * Returns null if not found.
     */
    public Restaurant findBySlug(String slug) {
        return Restaurant.find("slug", slug).firstResult();
    }

    /**
     * Finds a restaurant by slug or throws NotFoundException.
     */
    public Restaurant findBySlugOrThrow(String slug) {
        Restaurant restaurant = findBySlug(slug);
        if (restaurant == null) {
            throw new NotFoundException("Restaurant not found with slug: " + slug);
        }
        return restaurant;
    }
}

