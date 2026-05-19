package com.re.cinemamoviebookingsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class ErrorPageController {

    @GetMapping("/forbidden")
    public String forbidden() {
        return "error/forbidden";
    }

    @GetMapping("/business")
    public String business(Model model, @org.springframework.web.bind.annotation.ModelAttribute("errorMessage") String msg) {
        model.addAttribute("message", msg != null ? msg : "Đã xảy ra lỗi nghiệp vụ");
        return "error/business";
    }
}
