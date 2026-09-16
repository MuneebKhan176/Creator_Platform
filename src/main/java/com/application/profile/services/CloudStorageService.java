package com.application.profile.services;

import com.application.authentication.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

@Service
public class CloudStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicBaseUrl;

    public CloudStorageService(
            @Value("${app.storage.account-id}") String accountId,
            @Value("${app.storage.access-key-id}") String accessKeyId,
            @Value("${app.storage.secret-access-key}") String secretAccessKey,
            @Value("${app.storage.bucket-name}") String bucketName,
            @Value("${app.storage.public-base-url}") String publicBaseUrl) {

        this.bucketName = bucketName;

        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;

        // Cloudflare R2 uses an S3-compatible API.
        // "auto" is used because R2 does not use AWS regions.
        this.s3Client = S3Client.builder()
                .endpointOverride(
                        URI.create(
                                "https://" + accountId + ".r2.cloudflarestorage.com"
                        )
                )
                .region(Region.of("auto"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKeyId,
                                        secretAccessKey
                                )
                        )
                )
                .build();
    }

    /*
     * Existing method used by profile/image uploads.
     *
     * Keeps the old behavior:
     * - JPEG
     * - PNG
     * - WEBP
     * - Maximum 5 MB
     */
    public UploadedFile upload(MultipartFile file, String folder) {

        return upload(
                file,
                folder,
                ALLOWED_CONTENT_TYPES,
                MAX_FILE_SIZE_BYTES
        );
    }

    /*
     * New method used by PostService.
     *
     * Supports:
     * - Custom allowed content types
     * - Custom maximum file size
     *
     * Example from PostService:
     *
     * upload(file, "posts", allowedTypes, maxSize);
     */
    public UploadedFile upload(
            MultipartFile file,
            String folder,
            Set<String> allowedTypes,
            long maxSize) {

        if (file == null || file.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "No file was uploaded"
            );
        }

        if (allowedTypes == null || allowedTypes.isEmpty()) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No allowed file types were configured"
            );
        }

        if (maxSize <= 0) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid maximum file size configuration"
            );
        }

        /*
         * Validate file size.
         */
        if (file.getSize() > maxSize) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "File exceeds the maximum allowed size of "
                            + formatFileSize(maxSize)
            );
        }

        /*
         * Validate MIME type.
         */
        String contentType = file.getContentType();

        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported file type"
                            + (contentType != null
                            ? ": " + contentType
                            : "")
            );
        }

        /*
         * Determine file extension.
         */
        String extension = getExtension(contentType);

        /*
         * Generate unique R2 object key.
         *
         * Example:
         * posts/550e8400-e29b-41d4-a716-446655440000.jpg
         */
        String key = folder + "/"
                + UUID.randomUUID()
                + extension;

        try {

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),

                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );

        } catch (IOException e) {

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read the uploaded file"
            );

        } catch (Exception e) {

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload file. Please try again."
            );
        }

        /*
         * Return both:
         * - R2 object key
         * - Public URL
         */
        return new UploadedFile(
                key,
                publicBaseUrl + "/" + key
        );
    }

    /*
     * Get the correct file extension from MIME type.
     */
    private String getExtension(String contentType) {

        return switch (contentType) {

            case "image/jpeg" -> ".jpg";

            case "image/png" -> ".png";

            case "image/webp" -> ".webp";

            case "video/mp4" -> ".mp4";

            case "video/quicktime" -> ".mov";

            case "video/webm" -> ".webm";

            default -> "";
        };
    }

    /*
     * Format bytes into a readable value.
     *
     * Example:
     * 8388608 -> 8MB
     * 52428800 -> 50MB
     */
    private String formatFileSize(long bytes) {

        if (bytes >= 1024 * 1024) {
            return (bytes / (1024 * 1024)) + "MB";
        }

        if (bytes >= 1024) {
            return (bytes / 1024) + "KB";
        }

        return bytes + " bytes";
    }

    /*
     * Delete a file from Cloudflare R2.
     *
     * Used by PostService when an upload partially fails,
     * so already-uploaded files can be cleaned up.
     */
    public void delete(String key) {

        if (key == null || key.isBlank()) {
            return;
        }

        try {

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );

        } catch (Exception e) {

            /*
             * Best-effort cleanup.
             *
             * If deletion fails, don't fail the original request.
             */
        }
    }

    /*
     * Represents a successfully uploaded file.
     */
    public record UploadedFile(
            String key,
            String url
    ) {
    }
}