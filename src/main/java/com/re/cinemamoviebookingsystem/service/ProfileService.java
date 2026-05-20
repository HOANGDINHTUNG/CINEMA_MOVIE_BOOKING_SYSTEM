package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.ProfileUpdateRequest;
import com.re.cinemamoviebookingsystem.dto.response.ProfileDto;
import com.re.cinemamoviebookingsystem.entity.User;
import com.re.cinemamoviebookingsystem.entity.UserProfile;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.UserProfileRepository;
import com.re.cinemamoviebookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AvatarStorageService avatarStorageService;

    @Transactional(readOnly = true)
    public ProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "User không tồn tại"));
        UserProfile profile = user.getProfile();
        return ProfileDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(profile != null ? profile.getFullName() : "")
                .phoneNumber(profile != null ? profile.getPhoneNumber() : "")
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .roleName(user.getRole().getRoleName())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, ProfileUpdateRequest request, MultipartFile avatarFile) {
        UserProfile profile = userProfileRepository.findByUserUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Hồ sơ không tồn tại"));
        profile.setFullName(request.getFullName());
        profile.setPhoneNumber(request.getPhoneNumber());
        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarStorageService.deleteIfStored(profile.getAvatarUrl());
            profile.setAvatarUrl(avatarStorageService.storeAvatar(userId, avatarFile));
        }
        userProfileRepository.save(profile);
    }
}
