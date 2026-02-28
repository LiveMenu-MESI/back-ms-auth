package cloudSecurity.resource.admin;

import cloudSecurity.resource.BaseResource;

import cloudSecurity.dto.QRDTO;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.service.qr.QRCodeService;
import cloudSecurity.service.restaurant.RestaurantService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

import com.google.zxing.WriterException;

import io.quarkus.logging.Log;

/**
 * REST resource for QR code generation (CU-07).
 * Endpoints require authentication.
 */
@Path("/api/v1/admin/restaurants/{restaurantId}/qr")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class QRCodeResource extends BaseResource {

    @Inject
    QRCodeService qrCodeService;

    @Inject
    RestaurantService restaurantService;

    /**
     * Gets QR code information for preview.
     * GET /api/v1/admin/restaurants/{restaurantId}/qr
     */
    @GET
    public Response getQRInfo(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            Restaurant restaurant = restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);
            QRDTO.QRInfoResponse qrInfo = qrCodeService.getQRInfo(restaurant);
            return Response.ok(qrInfo).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Restaurant not found");
        } catch (Exception e) {
            Log.errorf(e, "Error getting QR info for restaurant: %s", restaurantId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get QR info"))
                    .build();
        }
    }

    /**
     * Generates and downloads a QR code.
     * GET /api/v1/admin/restaurants/{restaurantId}/qr/download
     * 
     * Query parameters:
     * - size: S, M, L, or XL (default: M)
     * - format: PNG or SVG (default: PNG)
     * - includeLogo: true/false (default: false)
     * - foregroundColor: hex color for QR modules (e.g. #000000, default black)
     * - backgroundColor: hex color for background (e.g. #FFFFFF, default white)
     */
    @GET
    @Path("/download")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, "image/png", "image/svg+xml"})
    public Response downloadQRCode(
            @HeaderParam("Authorization") String authorization,
            @PathParam("restaurantId") UUID restaurantId,
            @QueryParam("size") @DefaultValue("M") String sizeStr,
            @QueryParam("format") @DefaultValue("PNG") String formatStr,
            @QueryParam("includeLogo") @DefaultValue("false") Boolean includeLogo,
            @QueryParam("foregroundColor") String foregroundColor,
            @QueryParam("backgroundColor") String backgroundColor) {
        String userEmail = validateRestaurantOwnership(authorization, restaurantId);
        if (userEmail == null) {
            return unauthorized();
        }

        try {
            Restaurant restaurant = restaurantService.findByIdAndUserEmailOrThrow(restaurantId, userEmail);

            // Parse size
            QRDTO.QRSize size;
            try {
                size = QRDTO.QRSize.valueOf(sizeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return badRequest("Invalid size. Must be S, M, L, or XL");
            }

            // Parse format
            QRDTO.QRFormat format;
            try {
                format = QRDTO.QRFormat.valueOf(formatStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return badRequest("Invalid format. Must be PNG or SVG");
            }

            // Generate QR code (colors optional; invalid hex falls back to black/white)
            byte[] qrBytes = qrCodeService.generateQRCode(
                    restaurant, size, format, includeLogo,
                    foregroundColor, backgroundColor);

            // Build filename
            String filename = String.format("qr-%s-%s.%s", 
                    restaurant.slug, 
                    size.name().toLowerCase(), 
                    format.getExtension());

            // Return file
            return Response.ok(new ByteArrayInputStream(qrBytes))
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .type(format.getMimeType())
                    .build();

        } catch (jakarta.ws.rs.NotFoundException e) {
            return notFound("Restaurant not found");
        } catch (WriterException | java.io.IOException e) {
            Log.errorf(e, "Error generating QR code for restaurant: %s", restaurantId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to generate QR code"))
                    .build();
        } catch (UnsupportedOperationException e) {
            return badRequest(e.getMessage());
        }
    }
}

