package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Luôn resolve AppLanguage từ cookie/locale — tránh tham số controller bị null
 * (gây EL1007E: tmdbCode cannot be found on null).
 */
@Component
public class AppLanguageArgumentResolver implements HandlerMethodArgumentResolver {

    private final LocaleResolver localeResolver;

    public AppLanguageArgumentResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AppLanguage.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        AppLanguage lang = LocaleConfig.resolveAppLanguage(request, localeResolver);
        return lang != null ? lang : AppLanguage.VI_VN;
    }
}
