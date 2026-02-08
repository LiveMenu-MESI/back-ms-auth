package cloudSecurity.resource;

import cloudSecurity.base.BaseResourceTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for ImageResource endpoints.
 * Note: These tests may require mocking GCP Storage service.
 */
public class ImageResourceTest extends BaseResourceTest {

    @Test
    public void testUploadImage_Unauthenticated() {
        // Create a test image file
        File testImage = createTestImageFile();
        
        try {
            givenPublic()
                    .multiPart("image", testImage)
                    .when()
                    .post(BASE_PATH + "/images/upload")
                    .then()
                    .statusCode(401);
        } finally {
            testImage.delete();
        }
    }

    @Test
    public void testDeleteImage_Unauthenticated() {
        givenPublic()
                .when()
                .delete(BASE_PATH + "/images/test-image.jpg")
                .then()
                .statusCode(401);
    }

    @Test
    public void testGetImage_NoAuthRequired() {
        // Image redirect endpoint should work without auth
        givenPublic()
                .when()
                .get(BASE_PATH + "/images/large/test-image.jpg")
                .then()
                .statusCode(anyOf(is(200), is(302), is(404))); // Depends on implementation
    }

    private File createTestImageFile() {
        try {
            File tempFile = File.createTempFile("test-image", ".jpg");
            // Create a minimal valid JPEG (1x1 pixel)
            byte[] jpegHeader = new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                    0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                    (byte) 0xFF, (byte) 0xD9
            };
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(jpegHeader);
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test image", e);
        }
    }
}

