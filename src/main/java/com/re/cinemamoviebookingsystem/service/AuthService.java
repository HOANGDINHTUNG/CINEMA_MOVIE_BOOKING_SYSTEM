package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.RegisterRequest;
import com.re.cinemamoviebookingsystem.entity.Role;
import com.re.cinemamoviebookingsystem.entity.User;
import com.re.cinemamoviebookingsystem.entity.UserProfile;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.RoleRepository;
import com.re.cinemamoviebookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "Email đã được sử dụng");
        }
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Role CUSTOMER không tồn tại"));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(customerRole)
                .authProvider("LOCAL")
                .build();

        UserProfile profile = UserProfile.builder()
                .user(user)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .build();
        user.setProfile(profile);

        userRepository.save(user);
    }
}
