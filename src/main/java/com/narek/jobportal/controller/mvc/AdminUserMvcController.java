package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.AdminUserFilterDto;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserMvcController {

    private final UserService userService;

    public AdminUserMvcController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(@ModelAttribute("filter") AdminUserFilterDto filter,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate registeredFrom,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate registeredTo,
                            Model model) {
        filter.setRegisteredFrom(registeredFrom);
        filter.setRegisteredTo(registeredTo);

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        Page<User> users = Optional.ofNullable(userService.searchUsers(filter, pageable))
                .orElse(Page.empty(pageable));

        model.addAttribute("users", users);
        model.addAttribute("roles", Role.values());
        model.addAttribute("selectedRole", filter.getRole());
        return "dashboard/admin-users";
    }
    @GetMapping("/{id}")
    public String userDetails(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));
        model.addAttribute("user", user);
        return "dashboard/admin-user-details";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam boolean enabled,
                               RedirectAttributes redirectAttributes) {
        userService.setEnabled(id, enabled);
        redirectAttributes.addFlashAttribute("successMessage", enabled ? "User enabled." : "User disabled.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/bulk-status")
    public String bulkUpdateStatus(@RequestParam(name = "userIds", required = false) List<Long> userIds,
                                   @RequestParam boolean enabled,
                                   RedirectAttributes redirectAttributes) {
        if (userIds == null || userIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select at least one user.");
            return "redirect:/admin/users";
        }
        userService.setEnabledBulk(userIds, enabled);
        redirectAttributes.addFlashAttribute("successMessage", "Bulk user status updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted.");
        return "redirect:/admin/users";
    }
}
