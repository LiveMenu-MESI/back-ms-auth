package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for AnalyticsResource endpoints.
 */
public class AnalyticsResourceTest extends BaseResourceTest {

    private String restaurantId;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        restaurantId = createTestRestaurant();
    }

    private String createTestRestaurant() {
        return givenAuth()
                .body(Map.of("name", "Analytics Test Restaurant " + System.currentTimeMillis()))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    @Test
    public void testGetDashboard_Success() {
        givenAuth()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/analytics")
                .then()
                .statusCode(200)
                .body("restaurantId", equalTo(restaurantId))
                .body("summary", notNullValue());
    }

    @Test
    public void testGetDashboard_Unauthenticated() {
        givenPublic()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/analytics")
                .then()
                .statusCode(401);
    }

    @Test
    public void testExportAnalytics_Success() {
        givenAuth()
                .queryParam("startDate", "2024-01-01T00:00:00")
                .queryParam("endDate", "2024-12-31T23:59:59")
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/analytics/export")
                .then()
                .statusCode(200)
                .contentType(containsString("text/csv"));
    }

    @Test
    public void testExportAnalytics_Unauthenticated() {
        givenPublic()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/analytics/export")
                .then()
                .statusCode(401);
    }
}

