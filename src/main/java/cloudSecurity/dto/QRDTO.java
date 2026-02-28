package cloudSecurity.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTOs for QR code generation (CU-07).
 */
public class QRDTO {

    /** Default foreground (QR modules) and background colors (hex). */
    public static final String DEFAULT_FOREGROUND_HEX = "#000000";
    public static final String DEFAULT_BACKGROUND_HEX = "#FFFFFF";

    /**
     * QR code generation request parameters.
     * Colors are optional (hex e.g. #000000, #FFFFFF).
     */
    public record QRGenerationRequest(
            QRSize size,
            QRFormat format,
            Boolean includeLogo,
            String foregroundColor,
            String backgroundColor
    ) {
        public QRGenerationRequest(QRSize size, QRFormat format, Boolean includeLogo) {
            this(size, format, includeLogo, null, null);
        }
    }

    /**
     * QR code preview/info response.
     */
    public record QRInfoResponse(
            UUID restaurantId,
            String restaurantName,
            String slug,
            String qrUrl,
            String publicMenuUrl,
            QRSize defaultSize,
            List<QRSize> availableSizes,
            List<QRFormat> availableFormats,
            String defaultForegroundColor,
            String defaultBackgroundColor
    ) {
    }

    /**
     * QR code size options.
     */
    public enum QRSize {
        S(200, "Small (200x200 px)"),
        M(400, "Medium (400x400 px)"),
        L(800, "Large (800x800 px)"),
        XL(1200, "Extra Large (1200x1200 px)");

        private final int pixels;
        private final String description;

        QRSize(int pixels, String description) {
            this.pixels = pixels;
            this.description = description;
        }

        public int getPixels() {
            return pixels;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * QR code format options.
     */
    public enum QRFormat {
        PNG("image/png", "png"),
        SVG("image/svg+xml", "svg");

        private final String mimeType;
        private final String extension;

        QRFormat(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getExtension() {
            return extension;
        }
    }
}

