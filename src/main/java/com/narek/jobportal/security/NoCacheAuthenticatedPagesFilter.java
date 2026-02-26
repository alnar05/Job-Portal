package com.narek.jobportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class NoCacheAuthenticatedPagesFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticatedUser = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (authenticatedUser) {
            // Prevent browser reuse of pages from a previous logged-in session.
            // In normal tabs, cached pages/cookies can appear to "stick" to the old user,
            // while incognito works because it starts with a clean cache/cookie jar.
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        filterChain.doFilter(request, response);
    }
}