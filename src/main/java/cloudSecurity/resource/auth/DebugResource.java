package cloudSecurity.resource.auth;

import cloudSecurity.service.auth.TokenService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Debug resource to help diagnose token issues.
 * Shows what information is extracted from the token.
 */
@Path("/api/v1/auth/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    @Inject
    TokenService tokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Debug endpoint to see what email is extracted from the token.
     * GET /api/v1/auth/debug/token
     * No authentication required for debugging.
     */
    @GET
    @Path("/token")
    public Response debugToken(@HeaderParam("Authorization") String authorization) {
        String token = bearerToken(authorization);
        if (token == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "No token provided. Send Authorization: Bearer <token>"))
                    .build();
        }

        String email = tokenService.getUserEmailFromToken(token);
        boolean isValid = tokenService.isTokenValid(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("tokenProvided", token != null);
        response.put("tokenLength", token != null ? token.length() : 0);
        response.put("isValid", isValid);
        response.put("extractedEmail", email != null ? email : "null");
        response.put("hasEmail", email != null && !email.isBlank());
        
        // Try to decode JWT and show all claims
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
                response.put("jwtClaims", claims);
                response.put("jwtClaimKeys", claims.keySet());
            }
        } catch (Exception e) {
            response.put("jwtDecodeError", e.getMessage());
        }

        return Response.ok(response).build();
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
