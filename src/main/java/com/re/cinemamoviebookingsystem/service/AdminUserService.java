package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.AdminUserCreateRequest;
import com.re.cinemamoviebookingsystem.dto.request.AdminUserUpdateRequest;
import com.re.cinemamoviebookingsystem.dto.response.AdminUserListItemDto;
import com.re.cinemamoviebookingsystem.entity.Role;
import com.re.cinemamoviebookingsystem.entity.User;
import com.re.cinemamoviebookingsystem.entity.UserProfile;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.RoleRepository;
import com.re.cinemamoviebookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "STAFF", "CUSTOMER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AdminUserListItemDto> list(String roleName, String keyword, Pageable pageable) {
        String roleFilter = blankToNull(roleName);
        String kw = blankToNull(keyword);
        return userRepository.findForAdmin(roleFilter, kw, pageable).map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public AdminUserListItemDto get(Long userId) {
        User user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Không tìm thấy người dùng"));
        return toListItem(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminUserCreateRequest request) {
        validateRole(request.getRoleName());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "Email đã được sử dụng");
        }
        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Role không tồn tại"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .authProvider("LOCAL")
                .build();
        UserProfile profile = UserProfile.builder()
                .user(user)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .build();
        user.setProfile(profile);
        Long id = userRepository.save(user).getUserId();
        auditLogService.log("USER_CREATE", "USER", String.valueOf(id),
                request.getUsername() + " / " + request.getRoleName());
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, AdminUserUpdateRequest request, Long currentAdminId) {
        validateRole(request.getRoleName());
        User user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Không tìm thấy người dùng"));

        if (userId.equals(currentAdminId) && !"ADMIN".equals(request.getRoleName())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Không thể tự hạ quyền admin của chính mình");
        }

        String email = request.getEmail().trim();
        userRepository.findByEmail(email)
                .filter(u -> !u.getUserId().equals(userId))
                .ifPresent(u -> {
                    throw new BusinessException(ErrorCode.USER_EXISTS, "Email đã được sử dụng");
                });

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Role không tồn tại"));

        user.setEmail(email);
        user.setRole(role);
        if (user.getProfile() == null) {
            user.setProfile(UserProfile.builder().user(user).build());
        }
        user.getProfile().setFullName(request.getFullName());
        user.getProfile().setPhoneNumber(request.getPhoneNumber());

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }
        userRepository.save(user);
        auditLogService.log("USER_UPDATE", "USER", String.valueOf(userId),
                "role=" + request.getRoleName());
    }

    private void validateRole(String roleName) {
        if (roleName == null || !ALLOWED_ROLES.contains(roleName)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Vai trò không hợp lệ");
        }
    }

    private AdminUserListItemDto toListItem(User user) {
        return AdminUserListItemDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roleName(user.getRole().getRoleName())
                .fullName(user.getProfile() != null ? user.getProfile().getFullName() : "")
                .phoneNumber(user.getProfile() != null ? user.getProfile().getPhoneNumber() : "")
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
