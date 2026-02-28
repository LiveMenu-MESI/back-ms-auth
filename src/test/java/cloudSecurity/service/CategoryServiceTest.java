package cloudSecurity.service;

import cloudSecurity.dto.CategoryDTO;
import cloudSecurity.entity.Category;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.service.restaurant.CategoryService;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for CategoryService.
 */
@QuarkusTest
public class CategoryServiceTest {

    @Inject
    CategoryService categoryService;

    @Test
    @Transactional
    public void reorder_updatesPositionsInOrder() {
        Restaurant restaurant = new Restaurant("test@example.com", "Test Restaurant");
        restaurant.slug = "test-restaurant-" + UUID.randomUUID().toString().substring(0, 8);
        restaurant.persist();

        Category cat1 = new Category(restaurant, "Cat A", 1);
        cat1.persist();
        Category cat2 = new Category(restaurant, "Cat B", 2);
        cat2.persist();

        categoryService.reorder(restaurant.id, new CategoryDTO.ReorderCategoriesRequest(
                List.of(cat2.id, cat1.id)
        ));

        List<Category> after = categoryService.findAllByRestaurantId(restaurant.id);
        assertThat(after.size(), is(2));
        Category first = after.stream().filter(c -> c.id.equals(cat2.id)).findFirst().orElse(null);
        Category second = after.stream().filter(c -> c.id.equals(cat1.id)).findFirst().orElse(null);
        assertThat(first, notNullValue());
        assertThat(second, notNullValue());
        assertThat(first.position, is(1));
        assertThat(second.position, is(2));
    }

    @Test
    public void reorder_emptyCategoryIds_throws() {
        UUID restaurantId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.reorder(restaurantId, new CategoryDTO.ReorderCategoriesRequest(List.of())));
    }

    @Test
    public void reorder_nullCategoryIds_throws() {
        UUID restaurantId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
                categoryService.reorder(restaurantId, new CategoryDTO.ReorderCategoriesRequest(null)));
    }
}
