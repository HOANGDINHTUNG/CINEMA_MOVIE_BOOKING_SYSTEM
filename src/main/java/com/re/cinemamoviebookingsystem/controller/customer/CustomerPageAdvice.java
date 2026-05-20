package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.config.LocaleConfig;
import com.re.cinemamoviebookingsystem.service.ProfileService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.util.NavActive;
import com.re.cinemamoviebookingsystem.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.LocaleResolver;

@ControllerAdvice
public class CustomerPageAdvice {

    private final LocaleResolver localeResolver;
    private final ProfileService profileService;

    public CustomerPageAdvice(LocaleResolver localeResolver, ProfileService profileService) {
        this.localeResolver = localeResolver;
        this.profileService = profileService;
    }

    @ModelAttribute("appLang")
    public String appLang(HttpServletRequest request) {
        if (!isCustomerPath(request)) {
            return AppLanguage.VI_VN.getTmdbCode();
        }
        return resolve(request).getTmdbCode();
    }

    @ModelAttribute("appLanguage")
    public AppLanguage appLanguage(HttpServletRequest request) {
        if (!isCustomerPath(request)) {
            return AppLanguage.VI_VN;
        }
        return resolve(request);
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return relativePath(request);
    }

    @ModelAttribute("navActive")
    public String navActive(HttpServletRequest request) {
        if (!isCustomerPath(request)) {
            return "";
        }
        return NavActive.resolve(relativePath(request));
    }

    @ModelAttribute("accountSection")
    public String accountSection(HttpServletRequest request) {
        if (!isCustomerPath(request)) {
            return "";
        }
        return NavActive.accountSection(relativePath(request));
    }

    @ModelAttribute("headerDisplayName")
    public String headerDisplayName() {
        return headerProfileValue(ProfileDtoField.DISPLAY_NAME);
    }

    @ModelAttribute("headerAvatarUrl")
    public String headerAvatarUrl() {
        return headerProfileValue(ProfileDtoField.AVATAR);
    }

    @ModelAttribute("headerInitials")
    public String headerInitials() {
        return headerProfileValue(ProfileDtoField.INITIALS);
    }

    @ModelAttribute("langViUrl")
    public String langViUrl(HttpServletRequest request) {
        if (!isCustomerPath(request)) {
            return "/customer/lang?lang=vi-VN";
        }
        return langSwitchUrl("vi-VN", request);
    }

    @ModelAttribute("langEnUrl")
    public String langEnUrl(HttpServletRequest request) {
        if (!isCustomerPath(request)) {
            return "/customer/lang?lang=en-US";
        }
        return langSwitchUrl("en-US", request);
    }

    private enum ProfileDtoField { DISPLAY_NAME, AVATAR, INITIALS }

    private String headerProfileValue(ProfileDtoField field) {
        if (!isHeaderUserAuthenticated()) {
            return "";
        }
        try {
            var profile = profileService.getProfile(SecurityUtils.currentUserId());
            return switch (field) {
                case DISPLAY_NAME -> {
                    String name = profile.getFullName();
                    yield (name != null && !name.isBlank()) ? name : profile.getUsername();
                }
                case AVATAR -> profile.getAvatarUrl() != null ? profile.getAvatarUrl() : "";
                case INITIALS -> NavActive.initials(profile.getFullName(), profile.getUsername());
            };
        } catch (Exception ignored) {
            return headerProfileFallback(field);
        }
    }

    private String headerProfileFallback(ProfileDtoField field) {
        try {
            var user = SecurityUtils.currentUser();
            return switch (field) {
                case DISPLAY_NAME -> user.getUsername();
                case AVATAR -> "";
                case INITIALS -> NavActive.initials(null, user.getUsername());
            };
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isHeaderUserAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority();
            return "ROLE_CUSTOMER".equals(role)
                    || "ROLE_ADMIN".equals(role)
                    || "ROLE_STAFF".equals(role);
        });
    }

    private AppLanguage resolve(HttpServletRequest request) {
        AppLanguage lang = LocaleConfig.resolveAppLanguage(request, localeResolver);
        return lang != null ? lang : AppLanguage.VI_VN;
    }

    private static boolean isCustomerPath(HttpServletRequest request) {
        String path = relativePath(request);
        return path.startsWith("/customer");
    }

    private static String relativePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            return path.substring(ctx.length());
        }
        return path;
    }

    private static String langSwitchUrl(String lang, HttpServletRequest request) {
        String redirect = CustomerLanguageController.buildRedirectPath(request);
        return "/customer/lang?lang=" + lang + "&redirect="
                + CustomerLanguageController.encodeRedirect(redirect);
    }
}
