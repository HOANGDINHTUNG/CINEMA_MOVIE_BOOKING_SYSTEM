package com.re.cinemamoviebookingsystem.controller.auth;

import com.re.cinemamoviebookingsystem.dto.request.RegisterRequest;
import com.re.cinemamoviebookingsystem.security.RegistrationAuthSupport;
import com.re.cinemamoviebookingsystem.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationAuthSupport registrationAuthSupport;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(registerRequest);
            registrationAuthSupport.signInAfterRegistration(registerRequest.getUsername(), request, response);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký thành công! Chào mừng bạn — bạn đã được đăng nhập.");
            return "redirect:/customer/home";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }
}
