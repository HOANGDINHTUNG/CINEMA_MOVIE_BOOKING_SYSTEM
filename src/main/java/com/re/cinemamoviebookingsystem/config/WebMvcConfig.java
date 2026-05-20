package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.service.AvatarStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AvatarStorageService avatarStorageService;

    public WebMvcConfig(AvatarStorageService avatarStorageService) {
        this.avatarStorageService = avatarStorageService;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/customer/home");
        registry.addViewController("/customer").setViewName("redirect:/customer/home");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(
                        "classpath:/static/assets/"
                );
        String avatarLocation = avatarStorageService.getAvatarDirectory().toUri().toString();
        if (!avatarLocation.endsWith("/")) {
            avatarLocation += "/";
        }
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(avatarLocation);
    }
}
