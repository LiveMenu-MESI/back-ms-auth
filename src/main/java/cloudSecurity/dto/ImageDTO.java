package cloudSecurity.dto;

import java.util.Map;

/**
 * DTOs for image upload responses.
 */
public class ImageDTO {

    /**
     * Response containing processed image URLs.
     * Includes URLs for thumbnail, medium, and large variants.
     */
    public record ImageUploadResponse(
            String originalUrl,
            String thumbnailUrl,
            String mediumUrl,
            String largeUrl,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Metadata about the processed image.
     */
    public record ImageMetadata(
            String originalFormat,
            int originalWidth,
            int originalHeight,
            long originalSizeBytes,
            String processedFormat,
            Map<String, ImageVariantInfo> variants
    ) {
    }

    /**
     * Information about a specific image variant.
     */
    public record ImageVariantInfo(
            int width,
            int height,
            long sizeBytes,
            float quality
    ) {
    }
}

