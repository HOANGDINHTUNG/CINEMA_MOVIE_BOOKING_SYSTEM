package com.re.cinemamoviebookingsystem.dto.request;

import com.re.cinemamoviebookingsystem.validation.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "{auth.validation.username.required}")
    @Size(min = 3, max = 50, message = "{auth.validation.username.size}")
    private String username;

    @NotBlank(message = "{auth.validation.password.required}")
    @Size(min = 8, max = 100, message = "{auth.validation.password.size}")
    @StrongPassword
    private String password;

    @NotBlank(message = "{auth.validation.confirm_password.required}")
    private String confirmPassword;

    @NotBlank(message = "{auth.validation.email.required}")
    @Email(message = "{auth.validation.email.invalid}")
    private String email;

    @NotBlank(message = "{auth.validation.full_name.required}")
    @Size(max = 120, message = "{auth.validation.full_name.size}")
    private String fullName;

    @Size(max = 20, message = "{auth.validation.phone.size}")
    private String phoneNumber;

    @AssertTrue(message = "{auth.validation.password.mismatch}")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) {
            return true;
        }
        return password.equals(confirmPassword);
    }
}
