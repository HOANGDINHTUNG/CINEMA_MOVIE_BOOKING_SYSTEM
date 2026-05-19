package com.re.cinemamoviebookingsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcResolverConfig implements WebMvcConfigurer {

    private final AppLanguageArgumentResolver appLanguageArgumentResolver;

    public WebMvcResolverConfig(AppLanguageArgumentResolver appLanguageArgumentResolver) {
        this.appLanguageArgumentResolver = appLanguageArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(appLanguageArgumentResolver);
    }
}
