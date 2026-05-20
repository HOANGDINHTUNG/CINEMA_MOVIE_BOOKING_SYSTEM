package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.CinemaSettingsRequest;
import com.re.cinemamoviebookingsystem.service.AdminSettingsService;
import com.re.cinemamoviebookingsystem.service.TmdbHealthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;
    private final TmdbHealthService tmdbHealthService;

    @Value("${tmdb.api-key:}")
    private String tmdbApiKey;

    @GetMapping
    public String form(Model model) {
        model.addAttribute("settings", adminSettingsService.currentSettings());
        model.addAttribute("tmdbConfigured", tmdbApiKey != null && !tmdbApiKey.isBlank());
        return "admin/settings";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("settings") CinemaSettingsRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tmdbConfigured", tmdbApiKey != null && !tmdbApiKey.isBlank());
            return "admin/settings";
        }
        adminSettingsService.update(request);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã cập nhật cấu hình (có hiệu lực đến khi restart ứng dụng)");
        return "redirect:/admin/settings";
    }

    @GetMapping("/test-tmdb")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testTmdb() {
        var result = tmdbHealthService.testConnection();
        return ResponseEntity.ok(Map.of("ok", result.ok(), "message", result.message()));
    }
}
