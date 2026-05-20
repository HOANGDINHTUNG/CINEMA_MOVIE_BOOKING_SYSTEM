package com.re.cinemamoviebookingsystem.controller.auth;

import com.re.cinemamoviebookingsystem.dto.request.ProfileUpdateRequest;
import com.re.cinemamoviebookingsystem.service.ProfileService;
import com.re.cinemamoviebookingsystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping({"/customer/profile", "/staff/profile", "/admin/profile"})
    public String profile(Model model) {
        var profile = profileService.getProfile(SecurityUtils.currentUserId());
        model.addAttribute("profile", profile);
        if (!model.containsAttribute("profileUpdate")) {
            ProfileUpdateRequest req = new ProfileUpdateRequest();
            req.setFullName(profile.getFullName());
            req.setPhoneNumber(profile.getPhoneNumber());
            model.addAttribute("profileUpdate", req);
        }
        if ("/customer".equals(resolvePrefix())) {
            model.addAttribute("accountSection", "profile");
        }
        return resolveView("profile");
    }

    @PostMapping({"/customer/profile", "/staff/profile", "/admin/profile"})
    public String updateProfile(@Valid @ModelAttribute ProfileUpdateRequest profileUpdate,
                                BindingResult bindingResult,
                                @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                                RedirectAttributes redirectAttributes) {
        String prefix = resolvePrefix();
        if (bindingResult.hasErrors()) {
            return "redirect:" + prefix + "/profile";
        }
        try {
            profileService.updateProfile(SecurityUtils.currentUserId(), profileUpdate, avatar);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + prefix + "/profile";
    }

    private String resolveView(String name) {
        return resolvePrefix().substring(1) + "/" + name;
    }

    private String resolvePrefix() {
        String role = SecurityUtils.currentUser().getUser().getRole().getRoleName();
        return switch (role) {
            case "ADMIN" -> "/admin";
            case "STAFF" -> "/staff";
            default -> "/customer";
        };
    }
}
