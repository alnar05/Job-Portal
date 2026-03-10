package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/jobs")
public class JobMvcController {

    private final JobService jobService;

    public JobMvcController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listJobs(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String location,
                           @RequestParam(required = false) JobType jobType,
                           @RequestParam(required = false) Double minSalary,
                           @RequestParam(required = false) Double maxSalary,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "9") int size,
                           Model model) {
        Page<JobResponseDto> jobPage = jobService.searchJobs(
                keyword,
                location,
                jobType,
                minSalary,
                maxSalary,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );

        model.addAttribute("jobs", jobPage.getContent());
        model.addAttribute("jobPage", jobPage);
        model.addAttribute("jobTypes", List.of(JobType.values()));
        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);
        model.addAttribute("selectedJobType", jobType);
        model.addAttribute("minSalary", minSalary);
        model.addAttribute("maxSalary", maxSalary);
        return "jobs/list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String jobDetails(@PathVariable Long id, Model model) {
        JobResponseDto job = jobService.getJobById(id);
        model.addAttribute("job", job);
        if (!model.containsAttribute("applicationForm")) {
            model.addAttribute("applicationForm", new ApplicationCreateUpdateDto());
        }
        return "jobs/details";
    }

    @GetMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public String duplicateJob(@PathVariable Long id, Model model) {
        JobResponseDto job = jobService.getJobById(id);
        JobCreateUpdateDto jobForm = new JobCreateUpdateDto(
                job.getTitle(),
                job.getDescription(),
                job.getSalary(),
                job.getJobType(),
                job.getLocation(),
                null
        );
        model.addAttribute("jobForm", jobForm);
        model.addAttribute("successMessage", "Job duplicated into a new posting form. Set a new expiration date before posting.");
        return "jobs/create";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('EMPLOYER')")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("jobForm")) {
            model.addAttribute("jobForm", new JobCreateUpdateDto());
        }
        return "jobs/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('EMPLOYER')")
    public String createJob(@Valid @ModelAttribute("jobForm") JobCreateUpdateDto jobForm,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "jobs/create";
        }

        jobService.createJob(jobForm);
        redirectAttributes.addFlashAttribute("successMessage", "Job posted successfully.");
        return "redirect:/jobs";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public String showEditForm(@PathVariable Long id, Model model) {
        JobResponseDto job = jobService.getJobById(id);
        JobCreateUpdateDto jobForm = new JobCreateUpdateDto(job.getTitle(), job.getDescription(), job.getSalary(), job.getJobType(), job.getLocation(), job.getClosingDate());
        model.addAttribute("jobId", id);
        model.addAttribute("jobForm", jobForm);
        return "jobs/edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public String updateJob(@PathVariable Long id,
                            @Valid @ModelAttribute("jobForm") JobCreateUpdateDto jobForm,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("jobId", id);
            return "jobs/edit";
        }

        jobService.updateJob(id, jobForm);
        redirectAttributes.addFlashAttribute("successMessage", "Job updated successfully.");
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public String deleteJob(@PathVariable Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        jobService.deleteJob(id);
        redirectAttributes.addFlashAttribute("successMessage", "Job deleted successfully.");
        boolean isEmployer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYER"));

        return isEmployer ? "redirect:/employer/dashboard" : "redirect:/jobs";
    }
}