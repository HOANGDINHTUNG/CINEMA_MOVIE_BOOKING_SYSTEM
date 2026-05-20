package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserListItemDto {
    private Long userId;
    private String username;
    private String email;
    private String roleName;
    private String fullName;
    private String phoneNumber;
    private LocalDateTime createdAt;
}
