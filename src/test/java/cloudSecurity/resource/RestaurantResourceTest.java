package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for RestaurantResource endpoints.
 */
@QuarkusTest
public class RestaurantResourceTest extends BaseResourceTest {

    private String restaurantId;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // Create a test restaurant for use in tests
        restaurantId = createTestRestaurant();
    }

    private String createTestRestaurant() {
        String name = "Test Restaurant " + System.currentTimeMillis();
        
        String response = givenAuth()
                .body(Map.of(
                        "name", name,
                        "description", "Test description",
                        "phone", "+1234567890",
                        "address", "123 Test St"
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        return response;
    }

    @Test
    public void testCreateRestaurant_Success() {
        String name = "New Restaurant " + System.currentTimeMillis();
        
        givenAuth()
                .body(Map.of(
                        "name", name,
                        "description", "A new restaurant",
                        "phone", "+1234567890",
                        "address", "456 New St"
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo(name))
                .body("slug", notNullValue());
    }

    @Test
    public void testCreateRestaurant_Unauthenticated() {
        givenPublic()
                .body(Map.of("name", "Test Restaurant"))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(401);
    }

    @Test
    public void testGetAllRestaurants_Success() {
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test
    public void testGetRestaurantById_Success() {
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId)
                .then()
                .statusCode(200)
                .body("id", equalTo(restaurantId))
                .body("name", notNullValue());
    }

    @Test
    public void testGetRestaurantById_NotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + nonExistentId)
                .then()
                .statusCode(404);
    }

    @Test
    public void testUpdateRestaurant_Success() {
        String updatedName = "Updated Restaurant " + System.currentTimeMillis();
        
        givenAuth()
                .body(Map.of(
                        "name", updatedName,
                        "description", "Updated description"
                ))
                .when()
                .put(ADMIN_PATH + "/restaurants/" + restaurantId)
                .then()
                .statusCode(200)
                .body("id", equalTo(restaurantId))
                .body("name", equalTo(updatedName));
    }

    @Test
    public void testUpdateRestaurant_NotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        
        givenAuth()
                .body(Map.of("name", "Updated Name"))
                .when()
                .put(ADMIN_PATH + "/restaurants/" + nonExistentId)
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteRestaurant_Success() {
        // Create a restaurant to delete
        String idToDelete = createTestRestaurant();
        
        givenAuth()
                .when()
                .delete(ADMIN_PATH + "/restaurants/" + idToDelete)
                .then()
                .statusCode(204);
        
        // Verify it's deleted
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + idToDelete)
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteRestaurant_NotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        
        givenAuth()
                .when()
                .delete(ADMIN_PATH + "/restaurants/" + nonExistentId)
                .then()
                .statusCode(404);
    }
}


