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

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

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

        // R2's S3-compatible endpoint is account-scoped; region is arbitrary
        // ("auto") since R2 doesn't use AWS regions.
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    public UploadedFile upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No file was uploaded");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image must be smaller than 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, or WEBP images are allowed");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String key = folder + "/" + UUID.randomUUID() + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the uploaded file");
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image. Please try again.");
        }

        return new UploadedFile(key, publicBaseUrl + "/" + key);
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (Exception e) {
            // Best-effort cleanup of the old file — don't fail the request over it.
        }
    }

    public record UploadedFile(String key, String url) {
    }
}