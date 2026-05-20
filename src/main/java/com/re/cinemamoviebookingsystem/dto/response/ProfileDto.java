package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileDto {
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private String roleName;
}
