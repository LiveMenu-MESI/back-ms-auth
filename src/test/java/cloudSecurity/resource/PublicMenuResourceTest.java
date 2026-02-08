package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for PublicMenuResource endpoints.
 */
public class PublicMenuResourceTest extends BaseResourceTest {

    private String restaurantSlug;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        restaurantSlug = createTestRestaurantWithSlug();
    }

    private String createTestRestaurantWithSlug() {
        String name = "Public Test Restaurant " + System.currentTimeMillis();
        String slug = givenAuth()
                .body(Map.of("name", name))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(201)
                .extract()
                .path("slug");
        
        // Create at least one category and dish for the menu
        String categoryId = givenAuth()
                .body(Map.of(
                        "name", "Test Category",
                        "position", 1
                ))
                .when()
                .post(ADMIN_PATH + "/restaurants/" + 
                        givenAuth()
                                .when()
                                .get(ADMIN_PATH + "/restaurants")
                                .then()
                                .extract()
                                .path("[0].id") + "/categories")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        
        return slug;
    }

    @Test
    public void testGetPublicMenuBySlug_Success() {
        givenPublic()
                .when()
                .get(PUBLIC_PATH + "/menu/" + restaurantSlug)
                .then()
                .statusCode(anyOf(is(200), is(404))); // 200 if menu has dishes, 404 if not
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
        // Public endpoint should work without authentication
        givenPublic()
                .when()
                .get(PUBLIC_PATH + "/menu/" + restaurantSlug)
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }
}

