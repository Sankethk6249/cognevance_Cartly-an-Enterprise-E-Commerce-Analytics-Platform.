package com.cognevance.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.*;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class StorageService {
    @Value("${storage.provider}") private String provider;
    @Value("${storage.local-dir}") private String dir;
    @Value("${app.base-url}") private String baseUrl;
    @Value("${cloudinary.cloud-name}") private String cloud;
    @Value("${cloudinary.upload-preset}") private String preset;
    private final RestTemplate rest = new RestTemplate();

    public String store(MultipartFile f) throws IOException {
        String type = f.getContentType();
        if (f.isEmpty() || type == null || !type.startsWith("image/"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        return "cloudinary".equalsIgnoreCase(provider) ? toCloudinary(f) : toLocal(f);
    }

    private String toLocal(MultipartFile f) throws IOException {
        Path d = Paths.get(dir);
        Files.createDirectories(d);
        String ext = StringUtils.getFilenameExtension(f.getOriginalFilename());
        String safe = ext == null ? "" : "." + ext.toLowerCase().replaceAll("[^a-z0-9]", "");
        String name = UUID.randomUUID() + safe;
        Files.copy(f.getInputStream(), d.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return baseUrl + "/uploads/" + name;
    }

    private String toCloudinary(MultipartFile f) throws IOException {
        if (cloud.isBlank() || preset.isBlank())
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary is not configured");
        String filename = f.getOriginalFilename() == null ? "upload" : f.getOriginalFilename();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(f.getBytes()) {
            @Override public String getFilename() { return filename; }
        });
        body.add("upload_preset", preset);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        try {
            Map<?, ?> res = rest.postForObject("https://api.cloudinary.com/v1_1/" + cloud + "/image/upload",
                    new HttpEntity<>(body, h), Map.class);
            return String.valueOf(res.get("secure_url"));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image upload failed");
        }
    }
}