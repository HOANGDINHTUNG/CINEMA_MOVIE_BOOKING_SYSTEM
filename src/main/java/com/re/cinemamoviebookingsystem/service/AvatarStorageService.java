package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 2 * 1024 * 1024;

    private final Path avatarDir;

    public AvatarStorageService(@Value("${cinema.upload-dir:uploads}") String uploadDir) {
        this.avatarDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
    }

    public String storeAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Chưa chọn ảnh đại diện");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Ảnh tối đa 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Chỉ hỗ trợ JPG, PNG hoặc WebP");
        }
        String ext = extensionFor(contentType);
        try {
            Files.createDirectories(avatarDir);
            String filename = "user-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = avatarDir.resolve(filename);
            file.transferTo(target);
            return "/uploads/avatars/" + filename;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Không lưu được ảnh: " + ex.getMessage());
        }
    }

    public void deleteIfStored(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank() || !avatarUrl.startsWith("/uploads/avatars/")) {
            return;
        }
        String filename = avatarUrl.substring("/uploads/avatars/".length());
        try {
            Files.deleteIfExists(avatarDir.resolve(filename));
        } catch (IOException ignored) {
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    public Path getAvatarDirectory() {
        return avatarDir;
    }
}
