package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.*;
import com.narek.jobportal.entity.*;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        List<Job> jobs = jobService.searchAdminJobs(
                new AdminJobFilterDto(), PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
        List<ApplicationResponseDto> applications = applicationService.getAllApplications();
        List<User> employerUsers = userService.getUsersByRole(Role.EMPLOYER, PageRequest.of(0, 10_000))
                .getContent();
        List<User> candidateUsers = userService.getUsersByRole(Role.CANDIDATE, PageRequest.of(0, 10_000))
                .getContent();

        long activeJobs = 0;
        long closedJobs = 0;
        long expiredJobs = 0;

        for (Job job : jobs) {
            if (job.getEffectiveStatus() == JobStatus.CLOSED) {
                closedJobs++;
            } else if (job.getEffectiveStatus() == JobStatus.EXPIRED) {
                expiredJobs++;
            } else {
                activeJobs++;
            }
        }

        long pendingApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.REVIEWED
                        || a.getStatus() == ApplicationStatus.APPLIED)
                .count();

        long acceptedApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED)
                .count();

        long rejectedApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.REJECTED)
                .count();

        long cancelledApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.CANCELLED)
                .count();

        model.addAttribute("totalJobs", jobs.size());
        model.addAttribute("totalApplications", applications.size());
        model.addAttribute("employerUsers", employerUsers);
        model.addAttribute("candidateUsers", candidateUsers);

        AdminDashboardStatsDto stats = AdminDashboardStatsDto.builder()
                .totalJobs(jobs.size())
                .activeJobs(activeJobs)
                .closedJobs(closedJobs)
                .expiredJobs(expiredJobs)
                .totalApplications(applications.size())
                .pendingApplications(pendingApplications)
                .acceptedApplications(acceptedApplications)
                .rejectedApplications(rejectedApplications)
                .cancelledApplications(cancelledApplications)
                .totalUsers(employerUsers.size() + candidateUsers.size())
                .usersByRole(Map.of("EMPLOYER", (long) employerUsers.size(), "CANDIDATE", (long) candidateUsers.size()))
                .usersByStatus(Map.of(
                        "ENABLED", (long) employerUsers.stream()
                                .filter(User::isEnabled).count() + candidateUsers.stream().filter(User::isEnabled).count(),
                        "DISABLED", (long) employerUsers.stream()
                                .filter(u -> !u.isEnabled()).count() + candidateUsers.stream().filter(u -> !u.isEnabled()).count()))
                .jobsOverTime(buildMonthlyChartData(jobs, Job::getCreatedAt))
                .applicationsOverTime(buildMonthlyChartData(applications, ApplicationResponseDto::getAppliedAt))
                .usersOverTime(buildMonthlyChartData(
                        Stream
                                .concat(employerUsers.stream(), candidateUsers.stream()).toList(), User::getCreatedAt))
                .recentJobs(jobService.getRecentJobs(5).stream()
                        .map(job -> new ActivityItemDto(
                                job.getTitle(),
                                job.getEmployer().getCompanyName(),
                                String.valueOf(job.getCreatedAt())))
                        .toList())
                .recentApplications(applicationService.getRecentApplications(5).stream()
                        .map(application -> new ActivityItemDto(
                                application.getJobTitle(),
                                application.getCandidateName(),
                                String.valueOf(application.getAppliedAt())))
                        .toList())
                .recentUsers(userService.getRecentUsers(5).stream()
                        .map(user -> new ActivityItemDto(
                                user.getEmail(),
                                user.getRoles().stream().findFirst().map(Enum::name).orElse("USER"),
                                String.valueOf(user.getCreatedAt())))
                        .toList())
                .build();

        model.addAttribute("stats", stats);
        return "dashboard/admin";
    }

    @GetMapping({"/dashboard/employer", "/employer/dashboard"})
    @PreAuthorize("hasRole('EMPLOYER')")
    public String employerDashboard(Model model) {
        Employer employer = authService.getCurrentEmployer();
        List<JobResponseDto> jobs = jobService.getJobsByEmployerId(employer.getId());

        model.addAttribute("activeJobs", jobs.stream().filter(j -> j.getStatus() == JobStatus.ACTIVE).toList());
        model.addAttribute("expiredJobs", jobs.stream().filter(j -> j.getStatus() == JobStatus.EXPIRED).toList());
        model.addAttribute("closedJobs", jobs.stream().filter(j -> j.getStatus() == JobStatus.CLOSED).toList());
        return "dashboard/employer";
    }

    @GetMapping("/dashboard/candidate")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String candidateDashboard(Model model) {
        Candidate candidate = authService.getCurrentCandidate();
        model.addAttribute("applications", applicationService.getApplicationsByCandidateId(candidate.getId()));
        return "dashboard/candidate";
    }

    private <T> List<ChartPointDto> buildMonthlyChartData(List<T> source, Function<T, LocalDateTime> dateExtractor) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth now = YearMonth.now();

        Map<YearMonth, Long> counts = source.stream()
                .map(dateExtractor)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<ChartPointDto> points = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = now.minusMonths(i);
            points.add(new ChartPointDto(month.format(formatter), counts.getOrDefault(month, 0L)));
        }
        return points;
    }
}
