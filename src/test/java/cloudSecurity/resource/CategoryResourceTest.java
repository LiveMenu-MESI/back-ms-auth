package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for CategoryResource endpoints.
 */
public class CategoryResourceTest extends BaseResourceTest {

    private String restaurantId;
    private String categoryId;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        restaurantId = createTestRestaurant();
        categoryId = createTestCategory();
    }

    private String createTestRestaurant() {
        String name = "Test Restaurant " + System.currentTimeMillis();
        return givenAuth()
                .body(Map.of("name", name))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String createTestCategory() {
        String name = "Test Category " + System.currentTimeMillis();
        return givenAuth()
                .body(Map.of(
                        "name", name,
                        "description", "Test category description",
                        "position", 1
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    @Test
    public void testCreateCategory_Success() {
        String name = "New Category " + System.currentTimeMillis();
        
        givenAuth()
                .body(Map.of(
                        "name", name,
                        "description", "New category",
                        "position", 2
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo(name));
    }

    @Test
    public void testCreateCategory_Unauthenticated() {
        givenPublic()
                .body(Map.of("name", "Test Category"))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories")
                .then()
                .statusCode(401);
    }

    @Test
    public void testGetAllCategories_Success() {
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories")
                .then()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test
    public void testGetCategoryById_Success() {
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories/" + categoryId)
                .then()
                .statusCode(200)
                .body("id", equalTo(categoryId));
    }

    @Test
    public void testGetCategoryById_NotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories/" + nonExistentId)
                .then()
                .statusCode(404);
    }

    @Test
    public void testUpdateCategory_Success() {
        String updatedName = "Updated Category " + System.currentTimeMillis();
        
        givenAuth()
                .body(Map.of(
                        "name", updatedName,
                        "description", "Updated description"
                ))
                .when()
                .put(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories/" + categoryId)
                .then()
                .statusCode(200)
                .body("name", equalTo(updatedName));
    }

    @Test
    public void testDeleteCategory_Success() {
        // Create a category to delete (without dishes)
        String idToDelete = createTestCategory();
        
        givenAuth()
                .when()
                .delete(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories/" + idToDelete)
                .then()
                .statusCode(204);
    }

    @Test
    public void testReorderCategories_Success() {
        String category2Id = createTestCategory();
        givenAuth()
                .body(Map.of(
                        "categoryIds", List.of(category2Id, categoryId)
                ))
                .when()
                .patch(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories/reorder")
                .then()
                .statusCode(204);
    }

    @Test
    public void testReorderCategories_EmptyCategoryIds_Returns400() {
        givenAuth()
                .body(Map.of("categoryIds", List.of()))
                .when()
                .patch(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories/reorder")
                .then()
                .statusCode(400);
    }

    @Test
    public void testReorderCategories_NotFoundRestaurant_Returns404() {
        String nonExistentRestaurantId = UUID.randomUUID().toString();
        givenAuth()
                .body(Map.of("categoryIds", List.of(categoryId)))
                .when()
                .patch(ADMIN_PATH + "/restaurants/" + nonExistentRestaurantId + "/categories/reorder")
                .then()
                .statusCode(404);
    }

    @Test
    public void testCreateCategory_MissingName_Returns400() {
        givenAuth()
                .body(Map.of("description", "No name"))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + restaurantId + "/categories")
                .then()
                .statusCode(400);
    }
}

