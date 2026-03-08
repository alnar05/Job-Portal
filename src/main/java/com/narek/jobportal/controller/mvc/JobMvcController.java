package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.dto.SavedSearchDto;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.entity.SearchSortOption;
import com.narek.jobportal.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
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
                           @RequestParam(required = false) String companyName,
                           @RequestParam(defaultValue = "NEWEST") SearchSortOption sort,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "9") int size,
                           Model model) {
        Page<JobResponseDto> jobPage = jobService.searchJobs(
                keyword, location, jobType, minSalary, maxSalary, companyName, sort,
                PageRequest.of(page, size)
        );

        model.addAttribute("jobs", jobPage.getContent());
        model.addAttribute("jobPage", jobPage);
        model.addAttribute("jobTypes", List.of(JobType.values()));
        model.addAttribute("sortOptions", List.of(SearchSortOption.values()));
        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);
        model.addAttribute("companyName", companyName);
        model.addAttribute("selectedJobType", jobType);
        model.addAttribute("minSalary", minSalary);
        model.addAttribute("maxSalary", maxSalary);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("savedSearches", getSavedSearchesSafe());
        return "jobs/list";
    }

    @PostMapping("/save-search")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String saveSearch(@ModelAttribute SavedSearchDto savedSearchDto,
                             RedirectAttributes redirectAttributes) {
        jobService.saveSearch(savedSearchDto);
        redirectAttributes.addFlashAttribute("successMessage", "Search saved successfully.");
        return "redirect:/jobs";
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

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public String closeJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        jobService.closeJob(id);
        redirectAttributes.addFlashAttribute("successMessage", "Job closed successfully.");
        return "redirect:/jobs/" + id;
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
    public String deleteJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        jobService.deleteJob(id);
        redirectAttributes.addFlashAttribute("successMessage", "Job deleted successfully.");
        return "redirect:/jobs";
    }

    private List<SavedSearchDto> getSavedSearchesSafe() {
        try {
            return jobService.getSavedSearchesForCurrentCandidate();
        } catch (Exception ex) {
            return List.of();
        }
    }
}
