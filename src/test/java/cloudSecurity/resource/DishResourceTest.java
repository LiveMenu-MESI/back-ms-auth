package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for DishResource endpoints.
 */
@QuarkusTest
public class DishResourceTest extends BaseResourceTest {

    private String restaurantId;
    private String categoryId;
    private String dishId;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        restaurantId = createTestRestaurant();
        categoryId = createTestCategory();
        dishId = createTestDish();
    }

    private String createTestRestaurant() {
        return givenAuth()
                .body(Map.of("name", "Test Restaurant " + System.currentTimeMillis()))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String createTestCategory() {
        return givenAuth()
                .body(Map.of(
                        "name", "Test Category " + System.currentTimeMillis(),
                        "position", 1
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String createTestDish() {
        return givenAuth()
                .body(Map.of(
                        "name", "Test Dish " + System.currentTimeMillis(),
                        "description", "Test dish description",
                        "price", 10.99,
                        "categoryId", categoryId,
                        "available", true
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    @Test
    public void testCreateDish_Success() {
        String name = "New Dish " + System.currentTimeMillis();
        givenAuth()
                .body(Map.of(
                        "name", name,
                        "description", "New dish",
                        "price", 15.50,
                        "categoryId", categoryId,
                        "available", true
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo(name));
    }

    @Test
    public void testCreateDish_Unauthenticated() {
        givenPublic()
                .body(Map.of(
                        "name", "Dish",
                        "price", 10.0,
                        "categoryId", categoryId,
                        "available", true
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes")
                .then()
                .statusCode(401);
    }

    @Test
    public void testGetAllDishes_Success() {
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes")
                .then()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test
    public void testGetDishById_Success() {
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes/" + dishId)
                .then()
                .statusCode(200)
                .body("id", equalTo(dishId));
    }

    @Test
    public void testGetDishById_NotFound() {
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetAllDishes_Unauthenticated() {
        givenPublic()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes")
                .then()
                .statusCode(401);
    }

    @Test
    public void testUpdateDish_Success() {
        String updatedName = "Updated Dish " + System.currentTimeMillis();
        
        givenAuth()
                .body(Map.of(
                        "name", updatedName,
                        "price", 20.00
                ))
                .when()
                .put(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes/" + dishId)
                .then()
                .statusCode(200)
                .body("name", equalTo(updatedName));
    }

    @Test
    public void testToggleAvailability_Success() {
        givenAuth()
                .body(Map.of("available", false))
                .when()
                .patch(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes/" + dishId + "/availability")
                .then()
                .statusCode(200)
                .body("available", equalTo(false));
    }

    @Test
    public void testDeleteDish_Success() {
        String idToDelete = createTestDish();
        givenAuth()
                .when()
                .delete(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes/" + idToDelete)
                .then()
                .statusCode(204);
    }

    @Test
    public void testDeleteDish_NotFound() {
        givenAuth()
                .when()
                .delete(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    public void testUpdateDish_NotFound() {
        givenAuth()
                .body(Map.of("name", "Updated", "price", 99.0))
                .when()
                .put(ADMIN_PATH + "/restaurants/" + restaurantId + "/dishes/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}


