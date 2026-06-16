package me.alinizamani.byline.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.alinizamani.byline.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {
    private final S3Client s3Client;

    @Value("${app.aws.bucket.name}")
    String bucketName;

    @Override
    public String uploadAvatar(MultipartFile file, String mimeType) throws IOException {
        return uploadFile(file, "images/avatar/", mimeType);
    }

    @Override
    public InputStream getAvatar(String key) {
        return streamFile("images/avatar/" + key);
    }

    @Override
    public void deleteAvatar(String key) {
        deleteFile("images/avatar/" + key);
    }

    private void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    private String uploadFile(MultipartFile file, String path, String mimeType) throws IOException {
        log.info(mimeType);
        String fileKey = UUID.randomUUID() + toExtension(mimeType);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(path + fileKey)
                .contentType(mimeType)
                .build();


        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        return fileKey;
    }

    private InputStream streamFile(String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        return s3Client.getObject(request);
    }

    private String toExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/ogg" -> ".ogg";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
