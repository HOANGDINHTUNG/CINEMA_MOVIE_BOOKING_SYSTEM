package com.re.cinemamoviebookingsystem.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String redirectParam = request.getParameter("redirect");
        if (StringUtils.hasText(redirectParam)) {
            String target = redirectParam.startsWith("/") ? request.getContextPath() + redirectParam : redirectParam;
            response.sendRedirect(target);
            return;
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String target = savedRequest.getRedirectUrl();
            requestCache.removeRequest(request, response);
            response.sendRedirect(target);
            return;
        }

        String target = resolveRoleHome(authentication, request);
        response.sendRedirect(target);
    }

    private String resolveRoleHome(Authentication authentication, HttpServletRequest request) {
        String contextPath = request.getContextPath();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return contextPath + "/admin/dashboard";
            }
            if ("ROLE_STAFF".equals(role)) {
                return contextPath + "/staff/dashboard";
            }
        }
        return contextPath + "/customer/home";
    }

    public static String encodeContinueUrl(Long showtimeId) {
        return UriUtils.encode("/customer/booking/continue?showtimeId=" + showtimeId, StandardCharsets.UTF_8);
    }
}
