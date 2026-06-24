package com.aiteacher.service;

import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FileStorageService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.secure}")
    private boolean secure;

    public void initBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize bucket", e);
        }
    }

    public String uploadFile(MultipartFile file, String folder) {
        try {
            initBucket();
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = folder + "/" + UUID.randomUUID().toString() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public String uploadFile(MultipartFile file) {
        return uploadFile(file, "uploads");
    }

    public InputStream getFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file", e);
        }
    }

    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Get file as byte array from MinIO storage
     */
    public byte[] getFileBytes(String objectName) {
        try (InputStream stream = getFile(objectName)) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file bytes: " + objectName, e);
        }
    }

    public String getFileUrl(String objectName) {
        try {
            // Try SDK presigned URL first
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(3600 * 24) // 24 hours
                            .build()
            );
        } catch (Exception e) {
            // Fallback: construct URL directly (works for MinIO without SDK quirks)
            String base = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            return base + bucketName + "/" + objectName;
        }
    }

    /**
     * Upload a File to storage
     */
    public String uploadFile(File file, String folder, String originalFilename) {
        try {
            initBucket();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = folder + "/" + UUID.randomUUID().toString() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new FileInputStream(file), file.length(), -1)
                            .contentType("application/octet-stream")
                            .build()
            );

            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

}