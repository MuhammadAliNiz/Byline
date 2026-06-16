package me.alinizamani.byline.web.controller;

import lombok.RequiredArgsConstructor;
import me.alinizamani.byline.service.S3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    private final S3Service s3Service;

    @GetMapping("/avatar/{key}")
    public ResponseEntity<StreamingResponseBody> getAvatar(@PathVariable String key) {
        InputStream inputStream = s3Service.getAvatar(key);

        String contentType = resolveContentType(key);

        StreamingResponseBody responseBody = outputStream -> {
            byte[] buffer = new byte[8192];
            int bytesRead;
            try (inputStream) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(responseBody);
    }

    private String resolveContentType(String key) {
        if (key == null) {
            return "application/octet-stream";
        }

        String lowerKey = key.toLowerCase();

        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) return "image/jpeg";
        if (lowerKey.endsWith(".png"))  return "image/png";
        if (lowerKey.endsWith(".gif"))  return "image/gif";
        if (lowerKey.endsWith(".webp")) return "image/webp";

        if (lowerKey.endsWith(".mp4"))  return "video/mp4";
        if (lowerKey.endsWith(".webm")) return "video/webm";
        if (lowerKey.endsWith(".ogg") || lowerKey.endsWith(".ogv")) return "video/ogg";

        if (lowerKey.endsWith(".pdf"))  return "application/pdf";
        if (lowerKey.endsWith(".doc"))  return "application/msword";
        if (lowerKey.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        return "application/octet-stream";
    }
}
