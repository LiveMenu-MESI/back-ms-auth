package cloudSecurity.resource.admin;

import cloudSecurity.resource.BaseResource;

import cloudSecurity.dto.AnalyticsDTO;
import cloudSecurity.service.menu.AnalyticsService;
import cloudSecurity.service.restaurant.RestaurantService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

import io.quarkus.logging.Log;

/**
 * REST resource for analytics (optional feature).
 * Endpoints require authentication.
 */
@Path("/api/v1/admin/restaurants/{restaurantId}/analytics")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class AnalyticsResource extends BaseResource {

    @Inject
    AnalyticsService analyticsService;

    @Inject
    RestaurantService restaurantService;

    /**
     * Gets analytics dashboard for a restaurant.
     * GET /api/v1/admin/restaurants/{restaurantId}/analytics
     */
    @GET
    public Response getDashboard(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            AnalyticsDTO.AnalyticsDashboardResponse dashboard = analyticsService.getDashboard(restaurantId);
            return Response.ok(dashboard).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Restaurant not found");
        } catch (Exception e) {
            Log.errorf(e, "Error getting analytics for restaurant: %s", restaurantId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get analytics"))
                    .build();
        }
    }

    /**
     * Exports analytics data to CSV.
     * GET /api/v1/admin/restaurants/{restaurantId}/analytics/export
     * 
     * Query parameters:
     * - startDate: ISO date string (optional, default: 30 days ago)
     * - endDate: ISO date string (optional, default: now)
     */
    @GET
    @Path("/export")
    @Produces("text/csv")
    public Response exportAnalytics(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            LocalDateTime startDate = null;
            LocalDateTime endDate = null;
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

            if (startDateStr != null && !startDateStr.isBlank()) {
                try {
                    startDate = LocalDateTime.parse(startDateStr, formatter);
                } catch (DateTimeParseException e) {
                    return badRequest("Invalid startDate format. Use ISO 8601 format (e.g., 2024-01-01T00:00:00)");
                }
            }

            if (endDateStr != null && !endDateStr.isBlank()) {
                try {
                    endDate = LocalDateTime.parse(endDateStr, formatter);
                } catch (DateTimeParseException e) {
                    return badRequest("Invalid endDate format. Use ISO 8601 format (e.g., 2024-01-31T23:59:59)");
                }
            }

            String csv = analyticsService.exportToCSV(restaurantId, startDate, endDate);
            
            String filename = String.format("analytics-%s-%s.csv", 
                    restaurantId.toString().substring(0, 8),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            return Response.ok(csv)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .type("text/csv")
                    .build();

        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Restaurant not found");
        } catch (Exception e) {
            Log.errorf(e, "Error exporting analytics for restaurant: %s", restaurantId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to export analytics"))
                    .build();
        }
    }
}

