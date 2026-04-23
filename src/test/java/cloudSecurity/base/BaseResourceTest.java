package cloudSecurity.base;

import cloudSecurity.util.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for resource tests.
 * Provides common setup and utilities.
 *
 * @TestSecurity inyecta una identidad con rol "user" para todos los subtest,
 * eliminando la dependencia de un Keycloak real en la suite de tests.
 * Los tests de AuthResource que validan rechazo de tokens siguen funcionando
 * porque el endpoint hace su propia validación manual tras pasar @RolesAllowed.
 */
@QuarkusTest
@TestSecurity(user = "test@example.com", roles = {"user"})
public abstract class BaseResourceTest {

    protected static final String BASE_PATH = "/api/v1";
    protected static final String ADMIN_PATH = BASE_PATH + "/admin";
    protected static final String PUBLIC_PATH = BASE_PATH + "/public";
    protected static final String AUTH_PATH = BASE_PATH + "/auth";

    protected String testUserEmail = "test@example.com";
    protected String testToken;

    @BeforeEach
    public void setUp() {
        // Create a mock token for authenticated requests
        testToken = TestUtils.createMockJWT(testUserEmail);
    }

    /**
     * Gets the Authorization header with Bearer token.
     */
    protected String authHeader() {
        return TestUtils.bearerToken(testToken);
    }

    /**
     * Gets the Authorization header with a custom token.
     */
    protected String authHeader(String token) {
        return TestUtils.bearerToken(token);
    }

    /**
     * Sets up request with authentication.
     */
    protected io.restassured.specification.RequestSpecification givenAuth() {
        return io.restassured.RestAssured.given()
                .header("Authorization", authHeader())
                .contentType(ContentType.JSON);
    }

    /**
     * Sets up request without authentication (for public endpoints).
     */
    protected io.restassured.specification.RequestSpecification givenPublic() {
        return io.restassured.RestAssured.given()
                .contentType(ContentType.JSON);
    }
}

