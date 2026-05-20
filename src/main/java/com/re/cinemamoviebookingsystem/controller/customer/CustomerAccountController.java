package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.request.ChangePasswordRequest;
import com.re.cinemamoviebookingsystem.service.AccountService;
import com.re.cinemamoviebookingsystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer/account")
@RequiredArgsConstructor
public class CustomerAccountController {

    private final AccountService accountService;
    private final CinemaProperties cinemaProperties;

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("overview", accountService.getOverview(SecurityUtils.currentUserId()));
        model.addAttribute("accountSection", "overview");
        model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
        return "customer/account/overview";
    }

    @GetMapping("/security")
    public String security(Model model) {
        model.addAttribute("accountSection", "security");
        model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
        if (!model.containsAttribute("changePassword")) {
            model.addAttribute("changePassword", new ChangePasswordRequest());
        }
        return "customer/account/security";
    }

    @PostMapping("/security")
    public String changePassword(@Valid @ModelAttribute("changePassword") ChangePasswordRequest changePassword,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("accountSection", "security");
            model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
            return "customer/account/security";
        }
        try {
            accountService.changePassword(SecurityUtils.currentUserId(), changePassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công");
            return "redirect:/customer/account/security";
        } catch (Exception ex) {
            model.addAttribute("accountSection", "security");
            model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
            model.addAttribute("errorMessage", ex.getMessage());
            return "customer/account/security";
        }
    }
}
