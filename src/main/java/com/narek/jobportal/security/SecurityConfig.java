package com.narek.jobportal.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;

import static org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive.CACHE;
import static org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive.COOKIES;
import static org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive.STORAGE;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           NoCacheAuthenticatedPagesFilter noCacheAuthenticatedPagesFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(clearAllCookiesLogoutHandler())
                        .addLogoutHandler(clearSiteDataLogoutHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .permitAll()
                )
                .addFilterAfter(noCacheAuthenticatedPagesFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                );

        return http.build();
    }


    @Bean
    public LogoutHandler clearAllCookiesLogoutHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return;
            }

            for (Cookie cookie : cookies) {
                Cookie removeCookie = new Cookie(cookie.getName(), "");
                removeCookie.setMaxAge(0);
                removeCookie.setSecure(request.isSecure());
                removeCookie.setHttpOnly(cookie.isHttpOnly());
                removeCookie.setPath(cookie.getPath() != null ? cookie.getPath() : "/");
                response.addCookie(removeCookie);
            }
        };
    }

    @Bean
    public LogoutHandler clearSiteDataLogoutHandler() {
        return new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(COOKIES, CACHE, STORAGE));
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> response.sendRedirect("/login?logout");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}