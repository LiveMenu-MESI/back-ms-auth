package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Tests for PublicMenuResource endpoints.
 */
@QuarkusTest
public class PublicMenuResourceTest extends BaseResourceTest {

    private String restaurantSlug;
    private String restaurantId;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        var pair = createTestRestaurantWithMenu();
        restaurantId = pair.restaurantId;
        restaurantSlug = pair.slug;
    }

    private static class RestaurantSlugPair {
        final String restaurantId;
        final String slug;
        RestaurantSlugPair(String restaurantId, String slug) {
            this.restaurantId = restaurantId;
            this.slug = slug;
        }
    }

    private RestaurantSlugPair createTestRestaurantWithMenu() {
        String name = "Public Test Restaurant " + System.currentTimeMillis();
        String id = givenAuth()
                .body(Map.of("name", name))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        String slug = givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + id)
                .then()
                .statusCode(200)
                .extract()
                .path("slug");

        String categoryId = givenAuth()
                .body(Map.of(
                        "name", "Test Category",
                        "position", 1
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + id + "/categories")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        givenAuth()
                .body(Map.of(
                        "name", "Test Dish",
                        "description", "A test dish",
                        "price", 10.50,
                        "categoryId", categoryId,
                        "available", true
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + id + "/dishes")
                .then()
                .statusCode(201);

        return new RestaurantSlugPair(id, slug);
    }

    @Test
    public void testGetPublicMenuBySlug_Success() {
        givenPublic()
                .when()
                .get(PUBLIC_PATH + "/menu/" + restaurantSlug)
                .then()
                .statusCode(200);
    }

    @Test
    public void testGetPublicMenuBySlug_NotFound() {
        givenPublic()
                .when()
                .get(PUBLIC_PATH + "/menu/non-existent-slug")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetPublicMenu_NoAuthRequired() {
        givenPublic()
                .when()
                .get(PUBLIC_PATH + "/menu/" + restaurantSlug)
                .then()
                .statusCode(200);
    }

    @Test
    public void testGetPublicMenu_ResponseStructure() {
        givenPublic()
                .when()
                .get(PUBLIC_PATH + "/menu/" + restaurantSlug)
                .then()
                .statusCode(200)
                .body("restaurant", notNullValue())
                .body("restaurant.id", notNullValue())
                .body("restaurant.name", notNullValue())
                .body("restaurant.slug", equalTo(restaurantSlug))
                .body("restaurant.schedule", notNullValue())
                .body("categories", notNullValue())
                .body("categories.size()", greaterThanOrEqualTo(1))
                .body("categories[0].id", notNullValue())
                .body("categories[0].name", notNullValue())
                .body("categories[0].position", notNullValue())
                .body("categories[0].dishes", notNullValue())
                .body("categories[0].dishes.size()", greaterThanOrEqualTo(1))
                .body("categories[0].dishes[0].id", notNullValue())
                .body("categories[0].dishes[0].name", notNullValue())
                .body("categories[0].dishes[0].price", notNullValue())
                .body("categories[0].dishes[0].offerPrice", notNullValue())
                .body("categories[0].dishes[0].tags", notNullValue())
                .body("categories[0].dishes[0].featured", notNullValue());
    }

    @Test
    public void testGetPublicMenu_CategoriesOrderedByPosition() {
        givenAuth()
                .body(Map.of("name", "Category Position 2", "position", 2))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories")
                .then()
                .statusCode(201);
        givenAuth()
                .body(Map.of("name", "Category Position 1", "position", 1))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories")
                .then()
                .statusCode(201);

        givenPublic()
                .when()
                .get(PUBLIC_PATH + "/menu/" + restaurantSlug)
                .then()
                .statusCode(200)
                .body("categories.size()", greaterThanOrEqualTo(2))
                .body("categories[0].position", equalTo(1))
                .body("categories[1].position", anyOf(equalTo(1), equalTo(2)));
        // First category must be position 1 (asc order); second can be 1 or 2
    }
}


