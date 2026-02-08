package cloudSecurity.resource.admin;

import cloudSecurity.resource.BaseResource;

import cloudSecurity.dto.ImageDTO;
import cloudSecurity.service.storage.ImageService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.Map;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkus.logging.Log;

/**
 * REST resource for image upload and management (CU-05).
 * Handles image upload, processing, and storage in GCP Bucket.
 * Images are served directly from GCP Storage via public URLs.
 */
@Path("/api/v1/images")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class ImageResource extends BaseResource {

    @Inject
    ImageService imageService;

    /**
     * Uploads and processes an image.
     * POST /api/v1/images/upload
     * 
     * Accepts multipart/form-data with field name "image".
     * Returns URLs for thumbnail, medium, and large variants.
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadImage(
            @HeaderParam("Authorization") String authorization,
            @FormParam("image") FileUpload fileUpload) {
        
        // Validate authentication
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return unauthorized();
        }

        if (fileUpload == null) {
            return badRequest("Image file is required");
        }

        try {
            // Validate file
            String contentType = fileUpload.contentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return badRequest("File must be an image (JPEG, PNG, or WebP)");
            }

            // Process image
            try (InputStream inputStream = java.nio.file.Files.newInputStream(fileUpload.uploadedFile())) {
                ImageDTO.ImageUploadResponse response = imageService.processAndStoreImage(
                        inputStream,
                        contentType,
                        fileUpload.fileName()
                );
                return Response.ok(response).build();
            }
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            Log.errorf("Error uploading image: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to process image: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Note: Images are served directly from GCP Storage via public URLs.
     * This endpoint is kept for backward compatibility but redirects to GCP URLs.
     * 
     * GET /api/v1/images/{variant}/{filename}
     * 
     * Variants: original, thumbnail, medium, large
     */
    @GET
    @Path("/{variant}/{filename}")
    public Response serveImage(
            @PathParam("variant") String variant,
            @PathParam("filename") String filename) {
        
        // Validate variant
        if (!isValidVariant(variant)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid image variant"))
                    .build();
        }

        // Security: prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid filename"))
                    .build();
        }

        // Images are served directly from GCP Storage
        // Return a redirect or the GCP URL
        String gcsUrl = imageService.getImageUrl(variant, filename);
        return Response.temporaryRedirect(java.net.URI.create(gcsUrl)).build();
    }

    /**
     * Deletes an image and all its variants from GCS.
     * DELETE /api/v1/images/{filename}
     * 
     * @param filename The image filename (e.g., "uuid.jpg")
     */
    @DELETE
    @Path("/{filename}")
    public Response deleteImage(
            @HeaderParam("Authorization") String authorization,
            @PathParam("filename") String filename) {
        
        // Validate authentication
        String userEmail = getCurrentUserEmail(authorization);
        if (userEmail == null) {
            return unauthorized();
        }

        // Security: prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return badRequest("Invalid filename");
        }

        try {
            boolean deleted = imageService.deleteImage(filename);
            if (deleted) {
                return Response.noContent().build();
            } else {
                return notFound("Image not found");
            }
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            Log.errorf("Error deleting image: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to delete image: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Validates if the variant name is valid.
     */
    private boolean isValidVariant(String variant) {
        return variant != null && 
               (variant.equals("original") || 
                variant.equals("thumbnail") || 
                variant.equals("medium") || 
                variant.equals("large"));
    }
}
