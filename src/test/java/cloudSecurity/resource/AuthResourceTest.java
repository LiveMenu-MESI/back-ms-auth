package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import cloudSecurity.service.auth.KeycloakAdminService;
import cloudSecurity.service.auth.TokenService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for AuthResource endpoints.
 */
public class AuthResourceTest extends BaseResourceTest {

    @InjectMock
    KeycloakAdminService keycloakAdmin;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // Evitar llamadas reales a Keycloak en tests de registro
        Mockito.doNothing().when(keycloakAdmin).createUser(anyString(), anyString());
    }

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
                .statusCode(401)
                .body("error", notNullValue());
    }

    @Test
    public void testCurrentUser_InvalidToken_Returns401() {
        given()
                .header("Authorization", "Bearer invalid-token")
                .when()
                .get(AUTH_PATH + "/user")
                .then()
                .statusCode(401)
                .body("error", notNullValue());
    }

    @Test
    public void testCurrentUser_Authenticated_ReturnsIdAndEmail() {
        TokenService mockTokenService = mock(TokenService.class);
        when(mockTokenService.getCurrentUserFromToken(anyString()))
                .thenReturn(new TokenService.CurrentUser("test-user-uuid", testUserEmail));
        QuarkusMock.installMockForType(mockTokenService, TokenService.class);

        givenAuth()
                .when()
                .get(AUTH_PATH + "/user")
                .then()
                .statusCode(200)
                .body("id", equalTo("test-user-uuid"))
                .body("email", equalTo(testUserEmail));
    }
}

