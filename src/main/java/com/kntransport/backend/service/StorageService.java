package com.kntransport.backend.service;

import com.kntransport.backend.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @PostConstruct
    void logConfig() {
        if (isR2Configured()) {
            log.info("StorageService: R2 configured — account={} bucket={} publicUrl={}",
                    accountId, bucket, publicUrl);
        } else {
            log.warn("StorageService: R2 NOT configured (missing env vars) — falling back to local disk. " +
                    "accountId={} accessKeyId={} secretKey={} bucket={} publicUrl={}",
                    blank(accountId), blank(accessKeyId), blank(secretKey), blank(bucket), blank(publicUrl));
        }
    }

    private String blank(String s) { return (s == null || s.isBlank()) ? "MISSING" : "SET"; }

    @Value("${r2.account-id:}")        private String accountId;
    @Value("${r2.access-key-id:}")     private String accessKeyId;
    @Value("${r2.secret-key:}")        private String secretKey;
    @Value("${r2.bucket:}")            private String bucket;
    @Value("${r2.public-url:}")        private String publicUrl;
    @Value("${app.upload.dir:uploads}") private String uploadDir;

    /**
     * Stores the file and returns a permanent public URL.
     * @param folder    sub-folder name, e.g. "avatars" or "vehicles"
     * @param file      the uploaded multipart file
     * @param fixedName optional fixed filename (without extension) — if provided, the object
     *                  is always written to the same key so it replaces the previous upload.
     *                  Pass null to generate a random UUID filename.
     */
    public String store(String folder, MultipartFile file, String fixedName) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String ext      = getExtension(file.getOriginalFilename());
        String filename = (fixedName != null ? fixedName : UUID.randomUUID().toString()) + ext;
        String key      = folder + "/" + filename;

        if (isR2Configured()) {
            return uploadToR2(key, file);
        } else {
            return saveLocally(folder, filename, file);
        }
    }

    /** Convenience overload — generates a random UUID filename. */
    public String store(String folder, MultipartFile file) throws IOException {
        return store(folder, file, null);
    }

    /**
     * Deletes an object from R2 by its full public URL.
     * Safe to call even if the URL is null, blank, local, or the object doesn't exist.
     */
    public void deleteByUrl(String publicFileUrl) {
        if (publicFileUrl == null || publicFileUrl.isBlank()) return;
        if (!publicFileUrl.startsWith("http")) return; // local file — skip
        if (!isR2Configured()) return;

        // Extract the key from the URL: everything after the public URL base
        String base = (publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl);
        if (!publicFileUrl.startsWith(base + "/")) return;
        String key = publicFileUrl.substring(base.length() + 1);

        String endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";
        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretKey)))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build()) {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.info("Deleted R2 object: {}", key);
        } catch (Exception e) {
            log.warn("Could not delete R2 object {}: {}", key, e.getMessage());
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
        log.info("Uploading to R2: bucket={} key={} size={}", bucket, key, file.getSize());

        // Copy bytes to memory up front — MultipartFile stream can only be read once
        byte[] bytes = file.getBytes();
        // Normalise wildcard or missing content type — R2 must serve a specific MIME type
        // so Coil and browsers can decode the image without guessing
        String raw = file.getContentType();
        String contentType = (raw == null || raw.isBlank() || raw.equals("image/*"))
                ? "image/jpeg"
                : raw;

        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretKey)))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build()) {

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) bytes.length)
                    .build();

            s3.putObject(put, RequestBody.fromBytes(bytes));
        } catch (Exception e) {
            log.error("R2 upload failed for key={}: {}", key, e.getMessage(), e);
            throw new IOException("Failed to upload file to storage: " + e.getMessage(), e);
        }

        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        String url = base + "/" + key;
        log.info("R2 upload successful: {}", url);
        return url;
    }

    private String saveLocally(String folder, String filename, MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir, folder).toAbsolutePath();
        Files.createDirectories(dir);
        file.transferTo(dir.resolve(filename));
        return "/uploads/" + folder + "/" + filename;
    }

    /**
     * Diagnostic: attempts to write a tiny test object to R2 and returns a
     * human-readable result. Safe to call repeatedly — the test key is fixed
     * so it just overwrites itself.
     */
    public java.util.Map<String, String> testR2() {
        var result = new java.util.LinkedHashMap<String, String>();
        result.put("r2Configured", String.valueOf(isR2Configured()));
        result.put("accountId",    blank(accountId));
        result.put("accessKeyId",  blank(accessKeyId));
        result.put("secretKey",    blank(secretKey));
        result.put("bucket",       blank(bucket));
        result.put("publicUrl",    publicUrl);

        if (!isR2Configured()) {
            result.put("status", "SKIP — env vars missing");
            return result;
        }

        String endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";
        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretKey)))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build()) {

            byte[] data = "knt-r2-test".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key("_test/connectivity-check.txt")
                    .contentType("text/plain")
                    .contentLength((long) data.length)
                    .build(),
                RequestBody.fromBytes(data)
            );
            result.put("status", "OK — test object written successfully");
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            result.put("testUrl", base + "/_test/connectivity-check.txt");
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error",  e.getMessage());
            result.put("cause",  e.getCause() != null ? e.getCause().getMessage() : "none");
            log.error("R2 connectivity test failed: {}", e.getMessage(), e);
        }
        return result;
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
