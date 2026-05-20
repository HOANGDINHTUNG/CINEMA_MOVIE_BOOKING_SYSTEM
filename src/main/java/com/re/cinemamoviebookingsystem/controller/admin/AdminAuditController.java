package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var pageable = PageRequest.of(page, 30, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("logs", auditLogService.list(pageable));
        return "admin/audit/list";
    }
}
