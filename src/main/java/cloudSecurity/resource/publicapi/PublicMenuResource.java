package cloudSecurity.resource.publicapi;

import cloudSecurity.dto.PublicMenuDTO;
import cloudSecurity.service.menu.PublicMenuService;
import cloudSecurity.service.menu.AnalyticsService;
import cloudSecurity.service.restaurant.RestaurantService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.UUID;

import io.quarkus.logging.Log;

/**
 * REST resource for public menu display (CU-06).
 * This endpoint is public and does not require authentication.
 */
@Path("/api/v1/public/menu")
@Produces(MediaType.APPLICATION_JSON)
public class PublicMenuResource {

    @Inject
    PublicMenuService publicMenuService;

    @Inject
    AnalyticsService analyticsService;

    @Inject
    RestaurantService restaurantService;

    /**
     * Gets the public menu for a restaurant by slug.
     * GET /api/v1/public/menu/{slug}
     * 
     * This endpoint is public and does not require authentication.
     * Results are cached for performance.
     */
    @GET
    @Path("/{slug}")
    public Response getPublicMenu(
            @PathParam("slug") String slug,
            @HeaderParam("X-Forwarded-For") String forwardedFor,
            @HeaderParam("User-Agent") String userAgent) {
        try {
            PublicMenuDTO.PublicMenuResponse menu = publicMenuService.getPublicMenuBySlug(slug);
            
            // Record view for analytics (async, don't block response)
            try {
                UUID restaurantId = menu.restaurant().id();
                String ipAddress = forwardedFor != null ? forwardedFor.split(",")[0].trim() : null;
                analyticsService.recordMenuView(restaurantId, ipAddress, userAgent);
            } catch (Exception e) {
                // Don't fail the request if analytics fails
                Log.debugf("Failed to record menu view: %s", e.getMessage());
            }
            
            return Response.ok(menu).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Error retrieving public menu for slug: %s", slug);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve menu"))
                    .build();
        }
    }
}

