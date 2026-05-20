package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.ComboSaveRequest;
import com.re.cinemamoviebookingsystem.service.ComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/combos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminComboController {

    private final ComboService comboService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("combos", comboService.listAll());
        return "admin/combos/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        ComboSaveRequest req = new ComboSaveRequest();
        req.setStatus("ACTIVE");
        model.addAttribute("comboRequest", req);
        return "admin/combos/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("comboRequest") ComboSaveRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/combos/form";
        }
        try {
            comboService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo combo");
            return "redirect:/admin/combos";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/combos/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        var combo = comboService.get(id);
        ComboSaveRequest req = new ComboSaveRequest();
        req.setName(combo.getName());
        req.setDescription(combo.getDescription());
        req.setPrice(combo.getPrice());
        req.setStatus(combo.getStatus());
        model.addAttribute("comboRequest", req);
        model.addAttribute("comboId", id);
        return "admin/combos/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("comboRequest") ComboSaveRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("comboId", id);
            return "admin/combos/edit";
        }
        try {
            comboService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật combo");
            return "redirect:/admin/combos";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/combos/" + id + "/edit";
        }
    }
}
