package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    public static final String LANG_COOKIE = "APP_LANG";

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LANG_COOKIE);
        resolver.setDefaultLocale(Locale.forLanguageTag("vi-VN"));
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookiePath("/");
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * Ngôn ngữ UI và TMDB dùng chung cookie {@link #LANG_COOKIE} — không ưu tiên ?lang= trên URL
     * (tránh lệch: dữ liệu đổi mà menu vẫn tiếng Anh).
     */
    public static AppLanguage resolveAppLanguage(HttpServletRequest request, LocaleResolver localeResolver) {
        if (localeResolver != null && request != null) {
            Locale locale = localeResolver.resolveLocale(request);
            if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
                return AppLanguage.fromParam(locale.toLanguageTag());
            }
        }
        if (request != null) {
            String param = request.getParameter("lang");
            if (param != null && !param.isBlank()) {
                return AppLanguage.fromParam(param);
            }
        }
        return AppLanguage.VI_VN;
    }

    public static Optional<String> readLangCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (LANG_COOKIE.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public static void writeLangCookie(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (response == null || locale == null) {
            return;
        }
        Cookie cookie = new Cookie(LANG_COOKIE, locale.toLanguageTag());
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.ofDays(365).toSeconds());
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
    }
}
