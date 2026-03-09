package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.service.ApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employer/applications")
@PreAuthorize("hasAnyRole('EMPLOYER','ADMIN')")
public class EmployerApplicationMvcController {

    private final ApplicationService applicationService;

    public EmployerApplicationMvcController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#id)")
    public String accept(@PathVariable Long id,
                         @RequestHeader(value = "Referer", required = false) String referer,
                         RedirectAttributes redirectAttributes) {
        applicationService.acceptApplication(id);
        redirectAttributes.addFlashAttribute("successMessage", "Application accepted.");
        return redirectBack(referer, id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#id)")
    public String reject(@PathVariable Long id,
                         @RequestHeader(value = "Referer", required = false) String referer,
                         RedirectAttributes redirectAttributes) {
        applicationService.rejectApplication(id);
        redirectAttributes.addFlashAttribute("successMessage", "Application rejected.");
        return redirectBack(referer, id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#id)")
    public String cancel(@PathVariable Long id,
                         @RequestHeader(value = "Referer", required = false) String referer,
                         RedirectAttributes redirectAttributes) {
        applicationService.cancelApplicationDecision(id);
        redirectAttributes.addFlashAttribute("successMessage", "Application moved back to pending.");
        return redirectBack(referer, id);
    }

    private String redirectBack(String referer, Long applicationId) {
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/applications/" + applicationId;
    }
}
