package cloudSecurity.service.storage;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import io.quarkus.logging.Log;

/**
 * Service for uploading files to Google Cloud Storage using Service Account JSON key file.
 * Uses Google Cloud Storage client library for authentication and operations.
 */
@ApplicationScoped
public class GCPStorageService {

    @ConfigProperty(name = "gcp.storage.bucket.name")
    String bucketName;

    @ConfigProperty(name = "gcp.storage.credentials.path", defaultValue = "")
    String credentialsPath;

    private Storage storage;
    private boolean initialized = false;

    /**
     * Initializes the Google Cloud Storage client.
     * Uses service account JSON key file for authentication.
     * 
     */
    private void initializeStorage() throws IOException {
        if (initialized && storage != null) {
            return;
        }

        try {
            GoogleCredentials credentials = null;

                Log.infof("Loading GCP credentials from GOOGLE_APPLICATION_CREDENTIALS: %s", credentialsPath);
                if (credentialsPath != null && !credentialsPath.isBlank()) {
                    if (Files.exists(Paths.get(credentialsPath))) {
                        try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
                            credentials = GoogleCredentials.fromStream(serviceAccountStream)
                                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
                        }
                    } else {
                        Log.warnf("Credentials file not found at GOOGLE_APPLICATION_CREDENTIALS path: %s", credentialsPath);
                    }
                }

            storage = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();

            initialized = true;
            Log.infof("Google Cloud Storage client initialized successfully. Bucket: %s", bucketName);

        } catch (IOException e) {
            Log.errorf("Failed to initialize Google Cloud Storage client: %s", e.getMessage());
            throw new IOException("Failed to initialize GCP Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads a file to Google Cloud Storage using Service Account authentication.
     * 
     * @param objectPath The path/name of the object in the bucket (e.g., "images/thumbnail/photo.jpg")
     * @param content The file content as byte array
     * @param contentType The MIME type of the content
     * @throws IOException If upload fails
     */
    public void uploadFile(String objectPath, byte[] content, String contentType) throws IOException {
        try {
            if (bucketName == null || bucketName.isBlank()) {
                throw new IOException("GCP Storage bucket name is not configured");
            }

            initializeStorage();

            Log.debugf("Uploading to GCS: gs://%s/%s, Content-Type: %s, Size: %d bytes", 
                    bucketName, objectPath, contentType, content.length);

            BlobId blobId = BlobId.of(bucketName, objectPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();

            storage.create(blobInfo, content);

            Log.infof("Successfully uploaded file to GCS: gs://%s/%s", bucketName, objectPath);

        } catch (Exception e) {
            Log.errorf("Error uploading file to GCS: %s", e.getMessage(), e);
            throw new IOException("Failed to upload file to GCS: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the public URL for an object in GCS.
     * 
     * @param objectPath The path/name of the object in the bucket
     * @return Public URL for the object
     */
    public String getPublicUrl(String objectPath) {
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectPath);
    }

    /**
     * Deletes a file from Google Cloud Storage.
     * 
     * @param objectPath The path/name of the object in the bucket
     * @return true if the file was deleted, false if it didn't exist
     * @throws IOException If deletion fails
     */
    public boolean deleteFile(String objectPath) throws IOException {
        try {
            if (bucketName == null || bucketName.isBlank()) {
                throw new IOException("GCP Storage bucket name is not configured");
            }

            initializeStorage();

            Log.debugf("Deleting from GCS: gs://%s/%s", bucketName, objectPath);

            BlobId blobId = BlobId.of(bucketName, objectPath);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                Log.infof("Successfully deleted file from GCS: gs://%s/%s", bucketName, objectPath);
            } else {
                Log.warnf("File not found in GCS: gs://%s/%s", bucketName, objectPath);
            }

            return deleted;

        } catch (Exception e) {
            Log.errorf("Error deleting file from GCS: %s", e.getMessage(), e);
            throw new IOException("Failed to delete file from GCS: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the storage client is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
}
