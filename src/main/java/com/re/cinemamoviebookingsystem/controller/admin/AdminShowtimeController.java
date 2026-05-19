package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.ShowtimeCreateRequest;
import com.re.cinemamoviebookingsystem.service.LookupService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/showtimes")
@RequiredArgsConstructor
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;
    private final LookupService lookupService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("showtimes", showtimeService.listAllForAdmin());
        return "admin/showtimes/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("showtimeRequest", new ShowtimeCreateRequest());
        model.addAttribute("rooms", lookupService.listRooms());
        return "admin/showtimes/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("showtimeRequest") ShowtimeCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", lookupService.listRooms());
            return "admin/showtimes/form";
        }
        try {
            showtimeService.createShowtime(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo suất chiếu thành công");
            return "redirect:/admin/showtimes";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/showtimes/new";
        }
    }
}
