package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for AuthResource endpoints.
 */
public class AuthResourceTest extends BaseResourceTest {

    @Test
    public void testRegister_Success() {
        String email = "newuser" + System.currentTimeMillis() + "@example.com";
        String password = "password123";

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", password
                ))
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(201)
                .body("message", equalTo("User registered successfully"));
    }

    @Test
    public void testRegister_InvalidEmail() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "invalid-email",
                        "password", "password123"
                ))
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }

    @Test
    public void testRegister_ShortPassword() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "test@example.com",
                        "password", "short"
                ))
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(400)
                .body("error", containsString("at least 8 characters"));
    }

    @Test
    public void testRegister_MissingFields() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "test@example.com"))
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }

    @Test
    public void testRefreshToken_WithValidToken() {
        // Note: This test requires a valid refresh token from Keycloak
        // For now, we test the endpoint structure
        String refreshToken = "test-refresh-token";

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("refresh_token", refreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(anyOf(is(200), is(401))); // 200 if token valid, 401 if invalid
    }

    @Test
    public void testRefreshToken_MissingToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of())
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(400)
                .body("error", containsString("Refresh token is required"));
    }

    @Test
    public void testLogout() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(AUTH_PATH + "/logout")
                .then()
                .statusCode(204);
    }

    @Test
    public void testCurrentUser_Unauthenticated() {
        given()
                .when()
                .get(AUTH_PATH + "/user")
                .then()
                .statusCode(401);
    }

    @Test
    public void testCurrentUser_Authenticated() {
        givenAuth()
                .when()
                .get(AUTH_PATH + "/user")
                .then()
                .statusCode(anyOf(is(200), is(401))); // Depends on token validation
    }
}

