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

    /**
     * Gets a single dish by slug and dish ID (public, no auth).
     * Each call is recorded for "popular dishes" analytics.
     * GET /api/v1/public/menu/{slug}/dishes/{dishId}
     */
    @GET
    @Path("/{slug}/dishes/{dishId}")
    public Response getDishBySlug(
            @PathParam("slug") String slug,
            @PathParam("dishId") UUID dishId,
            @HeaderParam("X-Forwarded-For") String forwardedFor,
            @HeaderParam("User-Agent") String userAgent) {
        try {
            PublicMenuDTO.DishInfo dish = publicMenuService.getDishBySlug(slug, dishId);
            // Record dish view for analytics (popular dishes)
            try {
                String ipAddress = forwardedFor != null ? forwardedFor.split(",")[0].trim() : null;
                UUID restaurantId = restaurantService.findBySlugOrThrow(slug).id;
                analyticsService.recordDishView(restaurantId, dishId, ipAddress, userAgent);
            } catch (Exception e) {
                Log.debugf("Failed to record dish view: %s", e.getMessage());
            }
            return Response.ok(dish).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Error retrieving dish %s for slug %s", dishId, slug);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve dish"))
                    .build();
        }
    }
}

