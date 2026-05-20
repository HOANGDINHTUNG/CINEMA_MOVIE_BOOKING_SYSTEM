package com.re.cinemamoviebookingsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserUpdateRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String fullName;

    private String phoneNumber;

    @NotBlank
    private String roleName;

    private String newPassword;
}
