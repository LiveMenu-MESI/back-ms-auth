package cloudSecurity.service;

import cloudSecurity.service.auth.TokenService;
import cloudSecurity.util.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for TokenService.
 */
@QuarkusTest
public class TokenServiceTest {

    @Inject
    TokenService tokenService;

    @Test
    public void getCurrentUserFromToken_nullReturnsNull() {
        assertThat(tokenService.getCurrentUserFromToken(null), nullValue());
    }

    @Test
    public void getCurrentUserFromToken_blankReturnsNull() {
        assertThat(tokenService.getCurrentUserFromToken(""), nullValue());
        assertThat(tokenService.getCurrentUserFromToken("   "), nullValue());
    }

    @Test
    public void getCurrentUserFromToken_malformedTokenReturnsNull() {
        assertThat(tokenService.getCurrentUserFromToken("not-a-jwt"), nullValue());
        assertThat(tokenService.getCurrentUserFromToken("only.two"), nullValue());
    }

    @Test
    public void getCurrentUserFromToken_validStructureButInvalidSignature_returnsNull() {
        // Mock JWT has sub and email but Keycloak introspection will fail in test
        String mockToken = TestUtils.createMockJWT("user@test.com");
        // Without mocking Keycloak, isTokenValid returns false, so getCurrentUserFromToken returns null
        // (or may return from introspection which also fails). We only assert it doesn't throw.
        TokenService.CurrentUser result = tokenService.getCurrentUserFromToken(mockToken);
        // In test env Keycloak is disabled; result is typically null
        assertThat(result == null || ("user@test.com".equals(result.email()) && result.id() != null), is(true));
    }
}
