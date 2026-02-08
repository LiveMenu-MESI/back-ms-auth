package cloudSecurity.service.qr;

import cloudSecurity.dto.QRDTO;
import cloudSecurity.entity.Restaurant;
import cloudSecurity.service.storage.ImageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.logging.Log;

/**
 * Service for QR code generation (CU-07).
 */
@ApplicationScoped
public class QRCodeService {

    @ConfigProperty(name = "app.public.base.url", defaultValue = "http://localhost:8080")
    String publicBaseUrl;

    @Inject
    ImageService imageService;

    private static final ErrorCorrectionLevel ERROR_CORRECTION_LEVEL = ErrorCorrectionLevel.H; // 30%
    private static final int QUIET_ZONE = 4;

    /**
     * Generates a QR code image for a restaurant.
     * 
     * @param restaurant Restaurant to generate QR for
     * @param size QR code size
     * @param format Output format (PNG or SVG)
     * @param includeLogo Whether to include restaurant logo in center
     * @return Byte array of the QR code image
     */
    public byte[] generateQRCode(
            Restaurant restaurant,
            QRDTO.QRSize size,
            QRDTO.QRFormat format,
            Boolean includeLogo) throws IOException, WriterException {
        
        String qrUrl = buildQRUrl(restaurant.slug);
        int dimension = size.getPixels();

        // Generate QR code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION_LEVEL);
        hints.put(EncodeHintType.MARGIN, QUIET_ZONE);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix bitMatrix = qrCodeWriter.encode(qrUrl, BarcodeFormat.QR_CODE, dimension, dimension, hints);

        // Convert to image
        BufferedImage qrImage = matrixToImage(bitMatrix);

        // Add logo if requested
        if (includeLogo != null && includeLogo && restaurant.logo != null) {
            qrImage = addLogoToQR(qrImage, restaurant.logo);
        }

        // Convert to requested format
        return imageToBytes(qrImage, format);
    }

    /**
     * Gets QR code information for preview.
     */
    public QRDTO.QRInfoResponse getQRInfo(Restaurant restaurant) {
        String qrUrl = buildQRUrl(restaurant.slug);
        String publicMenuUrl = buildPublicMenuUrl(restaurant.slug);

        return new QRDTO.QRInfoResponse(
                restaurant.id,
                restaurant.name,
                restaurant.slug,
                qrUrl,
                publicMenuUrl,
                QRDTO.QRSize.M, // Default size
                Arrays.asList(QRDTO.QRSize.values()),
                Arrays.asList(QRDTO.QRFormat.values())
        );
    }

    /**
     * Builds the QR code URL.
     */
    private String buildQRUrl(String slug) {
        return publicBaseUrl + "/m/" + slug;
    }

    /**
     * Builds the public menu URL.
     */
    private String buildPublicMenuUrl(String slug) {
        return publicBaseUrl + "/m/" + slug;
    }

    /**
     * Converts BitMatrix to BufferedImage.
     */
    private BufferedImage matrixToImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        
        return image;
    }

    /**
     * Adds restaurant logo to the center of the QR code.
     */
    private BufferedImage addLogoToQR(BufferedImage qrImage, String logoUrl) {
        try {
            // Download logo
            BufferedImage logo = loadImageFromUrl(logoUrl);
            if (logo == null) {
                Log.warnf("Could not load logo from URL: %s", logoUrl);
                return qrImage;
            }

            int qrWidth = qrImage.getWidth();
            int qrHeight = qrImage.getHeight();
            
            // Logo size: 20% of QR code size
            int logoSize = (int) (qrWidth * 0.2);
            
            // Resize logo
            Image scaledLogo = logo.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH);
            BufferedImage resizedLogo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resizedLogo.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(scaledLogo, 0, 0, null);
            g2d.dispose();

            // Add white background for logo
            int padding = logoSize / 10;
            int logoWithPaddingSize = logoSize + (padding * 2);
            BufferedImage logoWithBackground = new BufferedImage(logoWithPaddingSize, logoWithPaddingSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D bgG2d = logoWithBackground.createGraphics();
            bgG2d.setColor(Color.WHITE);
            bgG2d.fillRect(0, 0, logoWithPaddingSize, logoWithPaddingSize);
            bgG2d.drawImage(resizedLogo, padding, padding, null);
            bgG2d.dispose();

            // Draw logo on QR code
            Graphics2D g = qrImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int x = (qrWidth - logoWithPaddingSize) / 2;
            int y = (qrHeight - logoWithPaddingSize) / 2;
            g.drawImage(logoWithBackground, x, y, null);
            g.dispose();

            return qrImage;
        } catch (Exception e) {
            Log.errorf(e, "Error adding logo to QR code: %s", e.getMessage());
            return qrImage; // Return QR without logo if logo fails
        }
    }

    /**
     * Loads an image from a URL.
     */
    private BufferedImage loadImageFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            try (InputStream inputStream = url.openStream()) {
                return ImageIO.read(inputStream);
            }
        } catch (Exception e) {
            Log.errorf(e, "Error loading image from URL: %s", urlString);
            return null;
        }
    }

    /**
     * Converts BufferedImage to byte array in the specified format.
     */
    private byte[] imageToBytes(BufferedImage image, QRDTO.QRFormat format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        if (format == QRDTO.QRFormat.SVG) {
            // For SVG, we need to convert the image to SVG format
            // This is a simplified approach - in production, you might want to use a proper SVG library
            throw new UnsupportedOperationException("SVG format not yet implemented. Please use PNG.");
        } else {
            // PNG format
            ImageIO.write(image, "PNG", baos);
        }
        
        return baos.toByteArray();
    }
}

