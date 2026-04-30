package com.kntransport.backend.service;

import com.kntransport.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Uploads files to Cloudflare R2 when R2 credentials are configured,
 * otherwise falls back to local disk (useful for local dev without R2).
 *
 * Required Railway env vars for R2:
 *   R2_ACCOUNT_ID      — Cloudflare account ID (from R2 dashboard)
 *   R2_ACCESS_KEY_ID   — R2 API token Access Key ID
 *   R2_SECRET_KEY      — R2 API token Secret Access Key
 *   R2_BUCKET          — bucket name (e.g. knt-uploads)
 *   R2_PUBLIC_URL      — public bucket URL (e.g. https://pub-xxxx.r2.dev)
 */
@Service
public class StorageService {

    @Value("${r2.account-id:}")        private String accountId;
    @Value("${r2.access-key-id:}")     private String accessKeyId;
    @Value("${r2.secret-key:}")        private String secretKey;
    @Value("${r2.bucket:}")            private String bucket;
    @Value("${r2.public-url:}")        private String publicUrl;
    @Value("${app.upload.dir:uploads}") private String uploadDir;

    /**
     * Stores the file and returns a permanent public URL.
     * @param folder  sub-folder name, e.g. "avatars" or "vehicles"
     * @param file    the uploaded multipart file
     */
    public String store(String folder, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        String key      = folder + "/" + filename;

        if (isR2Configured()) {
            return uploadToR2(key, file);
        } else {
            return saveLocally(folder, filename, file);
        }
    }

    private boolean isR2Configured() {
        return accountId  != null && !accountId.isBlank()  &&
               accessKeyId != null && !accessKeyId.isBlank() &&
               secretKey   != null && !secretKey.isBlank()   &&
               bucket      != null && !bucket.isBlank()       &&
               publicUrl   != null && !publicUrl.isBlank();
    }

    private String uploadToR2(String key, MultipartFile file) throws IOException {
        String endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";

        S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretKey)))
                // R2 requires path-style URLs — virtual-hosted style DNS does not resolve
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3.putObject(put, RequestBody.fromBytes(file.getBytes()));
        s3.close();

        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        return base + "/" + key;
    }

    private String saveLocally(String folder, String filename, MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir, folder).toAbsolutePath();
        Files.createDirectories(dir);
        file.transferTo(dir.resolve(filename));
        return "/uploads/" + folder + "/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
