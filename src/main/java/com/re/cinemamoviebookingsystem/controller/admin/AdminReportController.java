package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.service.ReportExportService;
import com.re.cinemamoviebookingsystem.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

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

    @GetMapping("/export/revenue")
    public void exportRevenue(@RequestParam(defaultValue = "0") int year,
                              @RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to,
                              HttpServletResponse response) throws Exception {
        int y = year > 0 ? year : LocalDate.now().getYear();
        LocalDate f = from != null ? from : LocalDate.of(y, 1, 1);
        LocalDate t = to != null ? to : LocalDate.of(y, 12, 31);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=bao-cao-doanh-thu.csv");
        try (var writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            reportExportService.writeRevenueCsv(writer, y, f, t);
        }
    }

    @GetMapping("/export/bookings")
    public void exportBookings(@RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to,
                               @RequestParam(required = false) BookingStatus status,
                               HttpServletResponse response) throws Exception {
        LocalDate f = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate t = to != null ? to : LocalDate.now();
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=don-dat-ve.csv");
        try (var writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            reportExportService.writeBookingsCsv(writer, f, t, status);
        }
    }
}
