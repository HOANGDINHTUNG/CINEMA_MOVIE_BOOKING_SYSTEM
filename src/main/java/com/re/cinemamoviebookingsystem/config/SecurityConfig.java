package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.security.LoginAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoginAuthenticationSuccessHandler loginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/error/**",
                                "/css/**", "/js/**", "/images/**", "/assets/**", "/webjars/**",
                                "/payment/vnpay-return",
                                "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/customer/booking/start").permitAll()
                        .requestMatchers(
                                "/customer",
                                "/customer/home",
                                "/customer/calendar",
                                "/customer/news", "/customer/news/**",
                                "/customer/promotions", "/customer/promotions/**",
                                "/customer/ticket-price",
                                "/customer/festival", "/customer/festival/**",
                                "/customer/catalog", "/customer/catalog/**",
                                "/customer/movies/**",
                                "/customer/lang"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/customer/showtimes/*/seats").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**").hasRole("STAFF")
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(loginEntryPoint())
                        .accessDeniedPage("/error/forbidden")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(loginSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/customer/home")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .requestCache(cache -> cache
                        .requestCache(new org.springframework.security.web.savedrequest.HttpSessionRequestCache()));
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint loginEntryPoint() {
        return new LoginUrlAuthenticationEntryPoint("/login");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
