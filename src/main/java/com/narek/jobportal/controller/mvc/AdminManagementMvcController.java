package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.UserStatus;
import com.narek.jobportal.service.AdminAuditService;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementMvcController {

    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final AdminAuditService adminAuditService;
    private final PasswordEncoder passwordEncoder;

    public AdminManagementMvcController(UserService userService, JobService jobService, ApplicationService applicationService, AdminAuditService adminAuditService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.adminAuditService = adminAuditService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        model.addAttribute("users", userService.getUsersByRole(Role.CANDIDATE, PageRequest.of(0, 50)).getContent());
        model.addAttribute("jobs", jobService.searchJobs(null, null, null, null, null, null, null, PageRequest.of(0, 100)).getContent());
        model.addAttribute("applications", applicationService.getAllApplications());
        return "dashboard/admin-panel";
    }

    @PostMapping("/users/{id}/status")
    public String updateUserStatus(@PathVariable Long id, @RequestParam UserStatus status, RedirectAttributes redirectAttributes) {
        userService.updateStatus(id, status);
        adminAuditService.log("USER_STATUS", "Updated user " + id + " to " + status);
        redirectAttributes.addFlashAttribute("successMessage", "User status updated.");
        return "redirect:/admin/panel";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam Role role, @RequestParam boolean add) {
        userService.updateRoles(id, role, add);
        adminAuditService.log("USER_ROLE", "User " + id + " role change " + role + " add=" + add);
        return "redirect:/admin/panel";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, passwordEncoder.encode(newPassword));
        adminAuditService.log("RESET_PASSWORD", "Password reset for user " + id);
        return "redirect:/admin/panel";
    }

    @PostMapping("/jobs/{id}/close")
    public String adminCloseJob(@PathVariable Long id) {
        jobService.closeJob(id);
        adminAuditService.log("CLOSE_JOB", "Closed job " + id);
        return "redirect:/admin/panel";
    }

    @PostMapping("/applications/{id}/notes")
    public String adminApplicationNotes(@PathVariable Long id, @RequestParam String notes) {
        applicationService.addInternalNotes(id, notes);
        adminAuditService.log("APPLICATION_NOTES", "Updated notes for application " + id);
        return "redirect:/admin/panel";
    }
}
