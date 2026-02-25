package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.service.JobService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jobs")
public class JobMvcController {

    private final JobService jobService;

    public JobMvcController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listJobs(Model model) {
        model.addAttribute("jobs", jobService.getAllJobs());
        return "jobs/list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String jobDetails(@PathVariable Long id, Model model) {
        JobResponseDto job = jobService.getJobById(id);
        model.addAttribute("job", job);
        return "jobs/details";
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
        JobCreateUpdateDto jobForm = new JobCreateUpdateDto(job.getTitle(), job.getDescription(), job.getSalary());
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
    public String deleteJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        jobService.deleteJob(id);
        redirectAttributes.addFlashAttribute("successMessage", "Job deleted successfully.");
        return "redirect:/jobs";
    }
}