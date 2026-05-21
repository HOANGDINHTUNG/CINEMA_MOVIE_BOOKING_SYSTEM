package com.re.cinemamoviebookingsystem.api;

import com.re.cinemamoviebookingsystem.dto.response.SchedulePageApiResponse;
import com.re.cinemamoviebookingsystem.controller.customer.CustomerSchedulePageService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicScheduleController {

    private final CustomerSchedulePageService customerSchedulePageService;

    @GetMapping("/schedule")
    public SchedulePageApiResponse schedule(
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false) List<String> age,
            @RequestParam(required = false) List<String> format,
            @RequestParam(required = false) List<String> origin,
            @RequestParam(required = false) Boolean available) {
        return customerSchedulePageService.loadSchedule(
                AppLanguage.fromParam(lang),
                date,
                q,
                genre,
                sort,
                age,
                format,
                origin,
                available);
    }
}
