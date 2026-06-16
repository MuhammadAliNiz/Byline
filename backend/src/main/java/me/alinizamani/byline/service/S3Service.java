package me.alinizamani.byline.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface S3Service {

    public String uploadAvatar(MultipartFile file, String mimeType) throws IOException;

    public InputStream getAvatar(String key);

    public void deleteAvatar(String key);

}
