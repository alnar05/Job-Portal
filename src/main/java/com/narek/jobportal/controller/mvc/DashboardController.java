package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ActivityItemDto;
import com.narek.jobportal.dto.AdminDashboardStatsDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.dto.ChartPointDto;
import com.narek.jobportal.entity.*;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isEmployer = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYER"));

        if (isAdmin) return new RedirectView("/dashboard/admin");
        if (isEmployer) return new RedirectView("/dashboard/employer");
        return new RedirectView("/dashboard/candidate");
    }

    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        List<Job> allJobs = jobService.searchAdminJobs(new com.narek.jobportal.dto.AdminJobFilterDto(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<ApplicationResponseDto> applications = applicationService.getAllApplications();
        List<User> users = userService.searchUsers(new com.narek.jobportal.dto.AdminUserFilterDto(), org.springframework.data.domain.Pageable.unpaged()).getContent();

        long activeJobs = allJobs.stream().filter(Job::isActive).count();
        long closedJobs = allJobs.stream().filter(j -> j.getStatus() == JobStatus.CLOSED).count();
        long expiredJobs = allJobs.stream().filter(Job::isExpired).count();

        Map<String, Long> usersByRole = Arrays.stream(Role.values())
                .collect(Collectors.toMap(Enum::name, r -> users.stream().filter(u -> u.getRoles().contains(r)).count()));
        Map<String, Long> usersByStatus = Map.of("ENABLED", users.stream().filter(User::isEnabled).count(), "DISABLED", users.stream().filter(u -> !u.isEnabled()).count());

        AdminDashboardStatsDto stats = AdminDashboardStatsDto.builder()
                .totalJobs(allJobs.size())
                .activeJobs(activeJobs)
                .closedJobs(closedJobs)
                .expiredJobs(expiredJobs)
                .totalApplications(applications.size())
                .pendingApplications(applications.stream().filter(a -> a.getStatus() == ApplicationStatus.APPLIED || a.getStatus() == ApplicationStatus.REVIEWED).count())
                .acceptedApplications(applications.stream().filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED).count())
                .rejectedApplications(applications.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count())
                .cancelledApplications(applications.stream().filter(a -> a.getStatus() == ApplicationStatus.CANCELLED).count())
                .totalUsers(users.size())
                .usersByRole(usersByRole)
                .usersByStatus(usersByStatus)
                .jobsOverTime(groupDates(allJobs.stream().collect(Collectors.groupingBy(j -> j.getCreatedAt().toLocalDate(), Collectors.counting()))))
                .applicationsOverTime(groupDates(applications.stream().collect(Collectors.groupingBy(a -> a.getAppliedAt().toLocalDate(), Collectors.counting()))))
                .usersOverTime(groupDates(users.stream().collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate(), Collectors.counting()))))
                .recentJobs(jobService.getRecentJobs(5).stream().map(j -> new ActivityItemDto(j.getTitle(), j.getEmployer().getCompanyName(), j.getCreatedAt().toString())).toList())
                .recentApplications(applicationService.getRecentApplications(5).stream().map(a -> new ActivityItemDto(a.getCandidateName(), a.getJobTitle(), a.getAppliedAt().toString())).toList())
                .recentUsers(userService.getRecentUsers(5).stream().map(u -> new ActivityItemDto(u.getEmail(), u.getRoles().toString(), u.getCreatedAt().toString())).toList())
                .build();

        model.addAttribute("stats", stats);
        return "dashboard/admin";
    }

    private List<ChartPointDto> groupDates(Map<LocalDate, Long> source) {
        return source.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new ChartPointDto(e.getKey().toString(), e.getValue())).toList();
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
