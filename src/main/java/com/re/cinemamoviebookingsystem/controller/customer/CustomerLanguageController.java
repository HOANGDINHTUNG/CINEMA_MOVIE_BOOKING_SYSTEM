package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.config.LocaleConfig;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/customer")
public class CustomerLanguageController {

    private final LocaleResolver localeResolver;

    public CustomerLanguageController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @GetMapping("/lang")
    public String switchLanguage(@RequestParam String lang,
                                 @RequestParam(required = false) String redirect,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        var locale = appLanguage.toLocale();
        localeResolver.setLocale(request, response, locale);
        LocaleConfig.writeLangCookie(request, response, locale);

        String target = (redirect != null && !redirect.isBlank()) ? redirect : "/customer/home";
        if (!target.startsWith("/customer")) {
            target = "/customer/home";
        }
        return "redirect:" + target;
    }

    static String buildRedirectPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String qs = request.getQueryString();
        if (qs != null && !qs.isBlank()) {
            return path + "?" + qs;
        }
        return path;
    }

    static String encodeRedirect(String redirectPath) {
        return URLEncoder.encode(redirectPath, StandardCharsets.UTF_8);
    }
}
