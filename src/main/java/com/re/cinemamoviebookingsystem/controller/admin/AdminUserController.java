package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.AdminUserCreateRequest;
import com.re.cinemamoviebookingsystem.dto.request.AdminUserUpdateRequest;
import com.re.cinemamoviebookingsystem.service.AdminUserService;
import com.re.cinemamoviebookingsystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public String list(@RequestParam(required = false) String role,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("users", adminUserService.list(role, q, pageable));
        model.addAttribute("roleFilter", role);
        model.addAttribute("keyword", q != null ? q : "");
        model.addAttribute("roles", new String[]{"ADMIN", "STAFF", "CUSTOMER"});
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("userRequest", new AdminUserCreateRequest());
        model.addAttribute("roles", new String[]{"ADMIN", "STAFF", "CUSTOMER"});
        return "admin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userRequest") AdminUserCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", new String[]{"ADMIN", "STAFF", "CUSTOMER"});
            return "admin/users/form";
        }
        try {
            Long id = adminUserService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo người dùng #" + id);
            return "redirect:/admin/users";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("roles", new String[]{"ADMIN", "STAFF", "CUSTOMER"});
            return "admin/users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var user = adminUserService.get(id);
        AdminUserUpdateRequest req = new AdminUserUpdateRequest();
        req.setEmail(user.getEmail());
        req.setFullName(user.getFullName());
        req.setPhoneNumber(user.getPhoneNumber());
        req.setRoleName(user.getRoleName());
        model.addAttribute("userRequest", req);
        model.addAttribute("userId", id);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("roles", new String[]{"ADMIN", "STAFF", "CUSTOMER"});
        return "admin/users/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userRequest") AdminUserUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", new String[]{"ADMIN", "STAFF", "CUSTOMER"});
            return "admin/users/edit";
        }
        try {
            adminUserService.update(id, request, SecurityUtils.currentUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật người dùng");
            return "redirect:/admin/users";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("roles", new String[]{"ADMIN", "STAFF", "CUSTOMER"});
            return "admin/users/edit";
        }
    }
}
