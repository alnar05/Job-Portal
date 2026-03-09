package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.AdminApplicationFilterDto;
import com.narek.jobportal.dto.AdminJobFilterDto;
import com.narek.jobportal.entity.ApplicationStatus;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.JobStatus;
import com.narek.jobportal.repository.EmployerRepository;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.JobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementMvcController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final EmployerRepository employerRepository;

    public AdminManagementMvcController(JobService jobService, ApplicationService applicationService, EmployerRepository employerRepository) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.employerRepository = employerRepository;
    }

    @GetMapping("/jobs")
    public String jobs(@ModelAttribute("filter") AdminJobFilterDto filter, Model model) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(filter.getDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC, filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        Page<Job> jobs = jobService.searchAdminJobs(filter, pageable);
        List<Long> ids = jobs.getContent().stream().map(Job::getId).toList();

        model.addAttribute("jobs", jobs);
        model.addAttribute("jobStatuses", JobStatus.values());
        model.addAttribute("employers", employerRepository.findAll());
        model.addAttribute("applicantCounts", applicationService.countByJobIds(ids));
        return "dashboard/admin-jobs";
    }

    @PostMapping("/jobs/bulk-status")
    public String bulkJobStatus(@RequestParam(name = "jobIds", required = false) List<Long> jobIds,
                                @RequestParam boolean reopen,
                                RedirectAttributes redirectAttributes) {
        if (jobIds == null || jobIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select at least one job.");
            return "redirect:/admin/jobs";
        }
        jobService.updateStatusBulk(jobIds, reopen);
        redirectAttributes.addFlashAttribute("successMessage", "Job statuses updated.");
        return "redirect:/admin/jobs";
    }

    @GetMapping("/applications")
    public String applications(@ModelAttribute("filter") AdminApplicationFilterDto filter, Model model) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(Sort.Direction.DESC, "appliedAt"));
        model.addAttribute("applicationPage", applicationService.searchAdminApplications(filter, pageable));
        model.addAttribute("applicationStatuses", ApplicationStatus.values());
        return "dashboard/admin-applications";
    }

    @PostMapping("/applications/{id}/status")
    public String updateApplicationStatus(@RequestParam ApplicationStatus status,
                                          @org.springframework.web.bind.annotation.PathVariable Long id,
                                          RedirectAttributes redirectAttributes) {
        switch (status) {
            case ACCEPTED -> applicationService.acceptApplication(id);
            case REJECTED -> applicationService.rejectApplication(id);
            case CANCELLED -> applicationService.cancelApplication(id);
            default -> applicationService.markAsReviewed(id);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Application status updated.");
        return "redirect:/admin/applications";
    }

    @PostMapping("/applications/bulk-status")
    public String bulkApplicationStatus(@RequestParam(name = "applicationIds", required = false) List<Long> ids,
                                        @RequestParam ApplicationStatus status,
                                        RedirectAttributes redirectAttributes) {
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select at least one application.");
            return "redirect:/admin/applications";
        }
        applicationService.updateStatusBulk(ids, status);
        redirectAttributes.addFlashAttribute("successMessage", "Application statuses updated.");
        return "redirect:/admin/applications";
    }
}
