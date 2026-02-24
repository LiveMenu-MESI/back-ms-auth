package cloudSecurity.service.storage;

import cloudSecurity.dto.ImageDTO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.ScalingMode;

import io.quarkus.logging.Log;

/**
 * Service for processing and storing images in Google Cloud Storage.
 * Handles image resizing, optimization, and storage in GCP Bucket using HMAC authentication.
 */
@ApplicationScoped
public class ImageService {

    @ConfigProperty(name = "app.images.base.url")
    Optional<String> baseUrl;

    @ConfigProperty(name = "app.images.max.size.mb", defaultValue = "5")
    int maxSizeMB;

    @Inject
    GCPStorageService gcpStorageService;

    private static final int MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/jpg", "image/png", "image/webp"};

    /**
     * Processes and stores an uploaded image in GCP Bucket.
     * Creates thumbnail (150x150), medium (400x400), and large (800x800) variants.
     * 
     * @param inputStream The image file input stream
     * @param contentType The MIME type of the image
     * @param originalFilename Original filename (for extension detection)
     * @return ImageUploadResponse with URLs for all variants
     */
    public ImageDTO.ImageUploadResponse processAndStoreImage(
            InputStream inputStream,
            String contentType,
            String originalFilename) throws IOException {
        
        if (!isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Invalid image type. Allowed types: JPEG, PNG, WebP");
        }

        byte[] imageBytes = inputStream.readAllBytes();
        
        if (imageBytes.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Image size exceeds maximum allowed size of " + maxSizeMB + "MB");
        }

        String fileId = UUID.randomUUID().toString();
        String extension = getExtensionFromContentType(contentType);
        String baseFilename = fileId + "." + extension;

        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid image file");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        Map<String, ImageDTO.ImageVariantInfo> variants = new HashMap<>();
        
        String thumbnailUrl = processAndUploadVariant(
                originalImage, baseFilename, "thumbnail", 150, 150, 0.80f, variants);
        
        String mediumUrl = processAndUploadVariant(
                originalImage, baseFilename, "medium", 400, 400, 0.85f, variants);
        
        String largeUrl = processAndUploadVariant(
                originalImage, baseFilename, "large", 800, 800, 0.90f, variants);

        String originalUrl = largeUrl; // Default to large
        if (originalWidth <= 800 && originalHeight <= 800) {
            String originalPath = "original/" + baseFilename;
            uploadToGCS(originalPath, imageBytes, contentType);
            originalUrl = getPublicUrl(originalPath);
        }

        ImageDTO.ImageMetadata metadata = new ImageDTO.ImageMetadata(
                contentType,
                originalWidth,
                originalHeight,
                imageBytes.length,
                "image/webp",
                variants
        );

        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("originalFormat", metadata.originalFormat());
        metadataMap.put("originalWidth", metadata.originalWidth());
        metadataMap.put("originalHeight", metadata.originalHeight());
        metadataMap.put("originalSizeBytes", metadata.originalSizeBytes());
        metadataMap.put("processedFormat", metadata.processedFormat());
        
        // Convert ImageVariantInfo records to Map for proper JSON serialization
        Map<String, Object> variantsMap = new HashMap<>();
        for (Map.Entry<String, ImageDTO.ImageVariantInfo> entry : metadata.variants().entrySet()) {
            ImageDTO.ImageVariantInfo variantInfo = entry.getValue();
            Map<String, Object> variantMap = new HashMap<>();
            variantMap.put("width", variantInfo.width());
            variantMap.put("height", variantInfo.height());
            variantMap.put("sizeBytes", variantInfo.sizeBytes());
            variantMap.put("quality", variantInfo.quality());
            variantsMap.put(entry.getKey(), variantMap);
        }
        metadataMap.put("variants", variantsMap);

        return new ImageDTO.ImageUploadResponse(
                originalUrl,
                thumbnailUrl,
                mediumUrl,
                largeUrl,
                metadataMap
        );
    }

    /**
     * Processes a single image variant and uploads it to GCP Storage.
     */
    private String processAndUploadVariant(
            BufferedImage originalImage,
            String baseFilename,
            String variantName,
            int width,
            int height,
            float quality,
            Map<String, ImageDTO.ImageVariantInfo> variants) throws IOException {
        
        try {
            BufferedImage resized = Thumbnails.of(originalImage)
                    .size(width, height)
                    .scalingMode(ScalingMode.BICUBIC)
                    .outputQuality(quality)
                    .asBufferedImage();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String outputFormat = "jpg"; // Default to JPEG
            String mimeType = "image/jpeg";

            try {
                String[] writerFormats = ImageIO.getWriterFormatNames();
                boolean webpAvailable = false;
                if (writerFormats != null) {
                    for (String format : writerFormats) {
                        if (format.equalsIgnoreCase("webp")) {
                            webpAvailable = true;
                            break;
                        }
                    }
                }

                if (webpAvailable) {
                    ImageIO.write(resized, "webp", outputStream);
                    outputFormat = "webp";
                    mimeType = "image/webp";
                } else {
                    ImageIO.write(resized, "jpg", outputStream);
                }
            } catch (Exception e) {
                ImageIO.write(resized, "jpg", outputStream);
            }

            byte[] imageBytes = outputStream.toByteArray();

            String variantFilename = baseFilename.replaceFirst("\\.(jpg|jpeg|png|webp)$", "." + outputFormat);
            String gcsPath = variantName + "/" + variantFilename;

            uploadToGCS(gcsPath, imageBytes, mimeType);

            variants.put(variantName, new ImageDTO.ImageVariantInfo(
                    resized.getWidth(),
                    resized.getHeight(),
                    imageBytes.length,
                    quality
            ));

            return getPublicUrl(gcsPath);
        } catch (IOException e) {
            Log.errorf("Error processing image variant %s: %s", variantName, e.getMessage());
            throw new IOException("Failed to process image variant: " + variantName, e);
        }
    }

    /**
     * Uploads image bytes to Google Cloud Storage using HMAC authentication.
     */
    private void uploadToGCS(String path, byte[] imageBytes, String contentType) throws IOException {
        gcpStorageService.uploadFile(path, imageBytes, contentType);
    }

    /**
     * Gets public URL for an image in GCS.
     * If baseUrl is configured, uses it; otherwise generates GCS public URL.
     */
    private String getPublicUrl(String gcsPath) {
        if (baseUrl.isPresent() && !baseUrl.get().trim().isEmpty()) {
            return baseUrl.get() + "/" + gcsPath;
        } else {
            return gcpStorageService.getPublicUrl(gcsPath);
        }
    }

    /**
     * Gets public URL for an image by variant and filename.
     * Public method for generating image URLs.
     */
    public String getImageUrl(String variant, String filename) {
        String gcsPath = variant + "/" + filename;
        return getPublicUrl(gcsPath);
    }

    /**
     * Validates if the content type is allowed.
     */
    private boolean isAllowedContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        contentType = contentType.toLowerCase();
        for (String allowed : ALLOWED_TYPES) {
            if (contentType.equals(allowed) || contentType.startsWith(allowed + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets file extension from content type.
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "jpg";
        }
        contentType = contentType.toLowerCase();
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return "jpg";
        } else if (contentType.contains("png")) {
            return "png";
        } else if (contentType.contains("webp")) {
            return "webp";
        }
        return "jpg";
    }

    /**
     * Deletes an image and all its variants from GCS.
     * 
     * @param filename The base filename (e.g., "uuid.jpg" or "uuid.webp")
     * @return true if at least one variant was deleted, false if none existed
     * @throws IOException If deletion fails
     */
    public boolean deleteImage(String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Security: prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Extract base name without extension for variant paths
        String baseName = filename;
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            baseName = filename.substring(0, lastDot);
        }

        boolean deletedAny = false;
        String[] variants = {"original", "thumbnail", "medium", "large"};

        // Try to delete all variants
        for (String variant : variants) {
            try {
                // Also try with different extensions (jpg, webp, png)
                String[] extensions = {"jpg", "webp", "png"};
                for (String ext : extensions) {
                    String variantPathWithExt = variant + "/" + baseName + "." + ext;
                    try {
                        if (gcpStorageService.deleteFile(variantPathWithExt)) {
                            deletedAny = true;
                        }
                    } catch (Exception e) {
                        // Continue with next variant/extension
                        Log.debugf("Could not delete variant %s: %s", variantPathWithExt, e.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.debugf("Error deleting variant %s: %s", variant, e.getMessage());
            }
        }

        return deletedAny;
    }
}
