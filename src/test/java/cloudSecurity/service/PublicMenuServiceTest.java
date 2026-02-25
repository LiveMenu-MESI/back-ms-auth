package cloudSecurity.service;

import cloudSecurity.dto.PublicMenuDTO;
import cloudSecurity.entity.Category;
import cloudSecurity.entity.Dish;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.service.menu.PublicMenuService;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for PublicMenuService.
 */
@QuarkusTest
public class PublicMenuServiceTest {

    @Inject
    PublicMenuService publicMenuService;

    @Test
    @Transactional
    public void getPublicMenuBySlug_returnsRestaurantCategoriesAndDishesOrderedByPosition() {
        Restaurant restaurant = new Restaurant("test@example.com", "Menu Test Restaurant");
        restaurant.persist();
        restaurant.slug = "menu-test-" + UUID.randomUUID().toString().substring(0, 8);

        Category cat1 = new Category(restaurant, "Starters", 2);
        cat1.persist();
        Category cat2 = new Category(restaurant, "Mains", 1);
        cat2.persist();

        Dish dish = new Dish();
        dish.restaurant = restaurant;
        dish.category = cat2;
        dish.name = "Main Dish";
        dish.price = BigDecimal.TEN;
        dish.offerPrice = BigDecimal.valueOf(9);
        dish.available = true;
        dish.position = 1;
        dish.persist();

        PublicMenuDTO.PublicMenuResponse menu = publicMenuService.getPublicMenuBySlug(restaurant.slug);

        assertThat(menu.restaurant(), notNullValue());
        assertThat(menu.restaurant().id(), is(restaurant.id));
        assertThat(menu.restaurant().slug(), is(restaurant.slug));
        assertThat(menu.restaurant().schedule(), notNullValue());
        assertThat(menu.categories().size(), is(2));
        assertThat(menu.categories().get(0).position(), is(1));
        assertThat(menu.categories().get(1).position(), is(2));
        assertThat(menu.categories().get(0).dishes().size() + menu.categories().get(1).dishes().size(), is(1));
    }

    @Test
    public void getPublicMenuBySlug_invalidSlug_throwsNotFound() {
        assertThrows(jakarta.ws.rs.NotFoundException.class, () ->
                publicMenuService.getPublicMenuBySlug("non-existent-slug-12345"));
    }
}
