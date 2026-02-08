package cloudSecurity.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for test helpers.
 */
public class TestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a mock JWT token for testing.
     * The token contains the email in the 'email' claim.
     */
    public static String createMockJWT(String email) {
        try {
            // Create a simple JWT payload
            Map<String, Object> header = Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            );

            Map<String, Object> payload = Map.of(
                    "sub", email,
                    "email", email,
                    "preferred_username", email,
                    "realm_access", Map.of(
                            "roles", new String[]{"user"}
                    ),
                    "exp", System.currentTimeMillis() / 1000 + 3600, // 1 hour from now
                    "iat", System.currentTimeMillis() / 1000
            );

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String headerB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Create a fake signature (for testing only)
            String signature = "test-signature";

            return headerB64 + "." + payloadB64 + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock JWT", e);
        }
    }

    /**
     * Creates a valid Bearer token header value.
     */
    public static String bearerToken(String token) {
        return "Bearer " + token;
    }

    /**
     * Generates a random UUID as string.
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a JSON string from a map.
     */
    public static String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to JSON", e);
        }
    }
}

