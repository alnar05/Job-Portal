package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class DashboardController {

    private final AuthService authService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final UserService userService;

    public DashboardController(AuthService authService,
                               JobService jobService,
                               ApplicationService applicationService,
                               UserService userService) {
        this.authService = authService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.userService = userService;
    }

    @GetMapping({"/dashboard"})
    @PreAuthorize("isAuthenticated()")
    public RedirectView dashboard(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isEmployer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYER"));

        if (isAdmin) {
            return new RedirectView("/dashboard/admin");
        }

        if (isEmployer) {
            return new RedirectView("/dashboard/employer");
        }

        return new RedirectView("/dashboard/candidate");

    }

    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        model.addAttribute("totalJobs", jobService.getAllJobs().size());
        model.addAttribute("totalApplications", applicationService.getAllApplications().size());
        model.addAttribute("employerUsers", userService.getUsersByRole(Role.EMPLOYER, PageRequest.of(0, 5)).getContent());
        model.addAttribute("candidateUsers", userService.getUsersByRole(Role.CANDIDATE, PageRequest.of(0, 5)).getContent());
        return "dashboard/admin";
    }

    @GetMapping({"/dashboard/employer", "/employer/dashboard"})
    @PreAuthorize("hasRole('EMPLOYER')")
    public String employerDashboard(Model model) {
        Employer employer = authService.getCurrentEmployer();
        model.addAttribute("jobs", jobService.getJobsByEmployerId(employer.getId()));
        return "dashboard/employer";
    }

    @GetMapping("/dashboard/candidate")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String candidateDashboard(Model model) {
        Candidate candidate = authService.getCurrentCandidate();
        model.addAttribute("applications", applicationService.getApplicationsByCandidateId(candidate.getId()));
        return "dashboard/candidate";
    }
}