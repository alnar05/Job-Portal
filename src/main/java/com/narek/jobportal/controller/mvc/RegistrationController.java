package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.RegistrationDto;
import com.narek.jobportal.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    @PreAuthorize("isAnonymous()")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationDto());
        }
        return "register";
    }

    @PostMapping("/register")
    @PreAuthorize("isAnonymous()")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationDto registrationForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            registrationService.register(registrationForm);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                bindingResult.rejectValue("email", "conflict", ex.getReason());
            } else {
                bindingResult.reject("registrationError", ex.getReason());
            }
            return "register";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Registration successful. Please sign in.");
        return "redirect:/login";
    }
}