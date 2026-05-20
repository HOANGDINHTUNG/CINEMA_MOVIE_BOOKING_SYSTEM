package com.re.cinemamoviebookingsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserCreateRequest {

    @NotBlank(message = "Tên đăng nhập không được trống")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email không được trống")
    @Email
    private String email;

    @NotBlank(message = "Mật khẩu không được trống")
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank(message = "Họ tên không được trống")
    private String fullName;

    private String phoneNumber;

    @NotBlank(message = "Vai trò không được trống")
    private String roleName;
}
