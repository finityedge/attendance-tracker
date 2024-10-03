package com.finityedge.attendancetracker;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class SecurityUtils {

    public static boolean isAdmin(Authentication authentication) {
        return authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public static void addAdminStatus(Model model, Authentication authentication) {
        model.addAttribute("isAdmin", isAdmin(authentication));
    }
}
