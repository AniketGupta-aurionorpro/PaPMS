package com.aurionpro.papms.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map<String, String> uploadFile(MultipartFile file, String folderName) {
        log.info("Uploading file '{}' to Cloudinary folder '{}'", file.getOriginalFilename(), folderName);
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folderName,
                    "original_filename", file.getOriginalFilename()
            ));

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("File uploaded successfully. Public ID: {}, URL: {}", publicId, url);

            return Map.of(
                    "url", url,
                    "public_id", publicId
            );
        } catch (IOException e) {
            log.error("Could not upload file '{}' to Cloudinary.", file.getOriginalFilename(), e);
            throw new RuntimeException("Could not upload file to Cloudinary", e);
        }
    }
}