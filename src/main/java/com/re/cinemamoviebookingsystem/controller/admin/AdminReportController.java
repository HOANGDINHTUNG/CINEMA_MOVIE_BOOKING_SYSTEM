package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public String report(@RequestParam(defaultValue = "0") int year,
                         @RequestParam(required = false) LocalDate from,
                         @RequestParam(required = false) LocalDate to,
                         Model model) {
        int y = year > 0 ? year : LocalDate.now().getYear();
        LocalDate f = from != null ? from : LocalDate.of(y, 1, 1);
        LocalDate t = to != null ? to : LocalDate.of(y, 12, 31);
        model.addAttribute("report", reportService.getReport(y, f, t));
        model.addAttribute("year", y);
        model.addAttribute("from", f);
        model.addAttribute("to", t);
        return "admin/reports";
    }
}
