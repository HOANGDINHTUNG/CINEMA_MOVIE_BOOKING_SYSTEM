package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.config.LocaleConfig;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.LocaleResolver;

@ControllerAdvice(basePackages = "com.re.cinemamoviebookingsystem.controller.customer")
public class CustomerPageAdvice {

    private final LocaleResolver localeResolver;

    public CustomerPageAdvice(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @ModelAttribute("appLang")
    public String appLang(HttpServletRequest request) {
        return resolve(request).getTmdbCode();
    }

    @ModelAttribute("appLanguage")
    public AppLanguage appLanguage(HttpServletRequest request) {
        return resolve(request);
    }

    private AppLanguage resolve(HttpServletRequest request) {
        AppLanguage lang = LocaleConfig.resolveAppLanguage(request, localeResolver);
        return lang != null ? lang : AppLanguage.VI_VN;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("langViUrl")
    public String langViUrl(HttpServletRequest request) {
        return langSwitchUrl("vi-VN", request);
    }

    @ModelAttribute("langEnUrl")
    public String langEnUrl(HttpServletRequest request) {
        return langSwitchUrl("en-US", request);
    }

    private static String langSwitchUrl(String lang, HttpServletRequest request) {
        String redirect = CustomerLanguageController.buildRedirectPath(request);
        return "/customer/lang?lang=" + lang + "&redirect="
                + CustomerLanguageController.encodeRedirect(redirect);
    }
}
