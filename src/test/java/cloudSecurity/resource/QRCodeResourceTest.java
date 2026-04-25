ackage cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for QRCodeResource endpoints.
 */
@QuarkusTest
public class QRCodeResourceTest extends BaseResourceTest {

    private String restaurantId;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        restaurantId = createTestRestaurant();
    }

    private String createTestRestaurant() {
        return givenAuth()
                .body(Map.of("name", "QR Test Restaurant " + System.currentTimeMillis()))
                .when()
                .post(ADMIN_PATH + "/restaurants")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    @Test
    public void testGetQRInfo_Success() {
        givenAuth()
                .queryParam("size", "M")
                .queryParam("format", "PNG")
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/qr")
                .then()
                .statusCode(200)
                .body("url", notNullValue())
                .body("size", notNullValue());
    }

    @Test
    public void testGetQRInfo_Unauthenticated() {
        givenPublic()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/qr")
                .then()
                .statusCode(401);
    }

    @Test
    public void testDownloadQR_Success() {
        givenAuth()
                .queryParam("size", "L")
                .queryParam("format", "PNG")
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/qr/download")
                .then()
                .statusCode(200)
                .contentType(containsString("image/png"));
    }

    @Test
    public void testDownloadQR_Unauthenticated() {
        givenPublic()
                .when()
                .get(ADMIN_PATH + "/restaurants/" + restaurantId + "/qr/download")
                .then()
                .statusCode(401);
    }
}


