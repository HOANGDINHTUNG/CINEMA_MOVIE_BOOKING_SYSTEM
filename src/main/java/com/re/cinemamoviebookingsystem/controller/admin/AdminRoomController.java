package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.RoomCreateRequest;
import com.re.cinemamoviebookingsystem.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final RoomService roomService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rooms", roomService.listAll());
        return "admin/rooms/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        RoomCreateRequest req = new RoomCreateRequest();
        req.setRows(8);
        req.setSeatsPerRow(12);
        req.setVipRowsFromEnd(2);
        model.addAttribute("roomRequest", req);
        return "admin/rooms/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("roomRequest") RoomCreateRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/rooms/form";
        }
        try {
            Integer id = roomService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo phòng #" + id);
            return "redirect:/admin/rooms";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/rooms/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("room", roomService.get(id));
        model.addAttribute("seats", roomService.listSeats(id));
        return "admin/rooms/detail";
    }
}
