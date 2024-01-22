package com.example.ContactDemo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class admin {

    @GetMapping("/admin_db")
    public String adminDashboardk()

    {
        return "admin/admin_dashboard";
    }

    @PreAuthorize("hasAuthority('Role_ADMIN')")
    @GetMapping("/orders")
    public String Dashboardk()
    {
        return "admin/orders";
    }
}
