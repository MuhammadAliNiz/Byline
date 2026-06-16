package me.alinizamani.byline.util;

import lombok.RequiredArgsConstructor;
import me.alinizamani.byline.exception.FileProcessingException;
import me.alinizamani.byline.exception.FileSizeExceededException;
import me.alinizamani.byline.exception.InvalidFileException;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FileValidator {

    // FIX 1: Removed 'static' so @Value can inject properly
    @Value("${app.MAX_IMAGE_SIZE}")
    private long MAX_IMAGE_SIZE;

    @Value("${app.MAX_VIDEO_SIZE}")
    private long MAX_VIDEO_SIZE;

    @Value("${app.MAX_DOCUMENT_SIZE}")
    private long MAX_DOCUMENT_SIZE;

    private final Tika tika = new Tika();

    private final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm",
            "video/ogg"
    );

    private final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public String validateImage(MultipartFile file) {
        return validate(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE);
    }

    public void validateVideo(MultipartFile file) {
        validate(file, ALLOWED_VIDEO_TYPES, MAX_VIDEO_SIZE);
    }

    public void validateDocument(MultipartFile file) {
        validate(file, ALLOWED_DOCUMENT_TYPES, MAX_DOCUMENT_SIZE);
    }

    private String validate(MultipartFile file, Set<String> allowedTypes, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File cannot be null or empty");
        }

        validateSize(file, maxSize);
        return validateMimeType(file, allowedTypes);
    }

    private void validateSize(MultipartFile file, long maxSize) {
        if(file.getSize() > maxSize) {
            // FIX 2: Added FileSizeExceededException
            throw new FileSizeExceededException(
                    String.format("File size %s exceeds maximum allowed %s",
                            readableSize(file.getSize()),
                            readableSize(maxSize))
            );
        }
    }

    private String validateMimeType(MultipartFile file, Set<String> allowedTypes) {
        try {
            String detectedType = tika.detect(file.getInputStream());

            if (!allowedTypes.contains(detectedType)) {
                throw new InvalidFileException(
                        "File format '" + detectedType + "' is not supported. " +
                                "Allowed formats: " + allowedTypes
                );
            }
            return detectedType;
        } catch (IOException e) {
            throw new FileProcessingException("Could not read file for validation");
        }
    }

    private String readableSize(long bytes) {
        if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)) + "MB";
        if (bytes >= 1024) return (bytes / 1024) + "KB";
        return bytes + "B";
    }
}