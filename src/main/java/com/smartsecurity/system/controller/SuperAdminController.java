package com.smartsecurity.system.controller;

import com.smartsecurity.system.dto.SecurityUserRequest;
import com.smartsecurity.system.dto.TenantAdminRequest;
import com.smartsecurity.system.dto.TenantRequest;
import com.smartsecurity.system.dto.TenantResponse;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.entity.VisitorHistory;
import com.smartsecurity.system.service.ReportService;
import com.smartsecurity.system.service.TenantService;
import com.smartsecurity.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final TenantService tenantService;
    private final UserService userService;
    private final ReportService reportService;

    // @GetMapping("/tenants")
    // public ResponseEntity<List<Tenant>> getAllTenants() {
    //     return ResponseEntity.ok(tenantService.getAllTenants());
    // }

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @PostMapping("/tenants")
    public ResponseEntity<Tenant> createTenant(@RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.createTenant(request));
    }

    @PostMapping("/tenants/{id}/admins")
    public ResponseEntity<User> addTenantAdmin(@PathVariable Long id, @RequestBody TenantAdminRequest request) {
        return ResponseEntity.ok(tenantService.addTenantAdmin(id, request));
    }

    @GetMapping("/tenants/{id}/admins")
    public ResponseEntity<List<User>> getTenantAdmins(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.getTenantAdmins(id));
    }

    @PutMapping("/tenants/admins/{adminId}")
    public ResponseEntity<User> updateTenantAdmin(
            @PathVariable Long adminId,
            @RequestBody TenantAdminRequest request) {
        return ResponseEntity.ok(tenantService.updateTenantAdmin(adminId, request));
    }

    @DeleteMapping("/tenants/admins/{adminId}")
    public ResponseEntity<Map<String, String>> deleteTenantAdmin(@PathVariable Long adminId) {
        tenantService.deleteTenantAdmin(adminId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Tenant admin deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/tenants/{id}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable Long id, @RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @DeleteMapping("/tenants/{id}")
    public ResponseEntity<Map<String, String>> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Tenant deleted successfully");
        return ResponseEntity.ok(response);
    }

    // Security User Management
    @GetMapping("/security-users")
    public ResponseEntity<List<User>> getAllSecurityUsers() {
        return ResponseEntity.ok(userService.getSecurityUsers());
    }

    @PostMapping("/security-users")
    public ResponseEntity<User> createSecurityUser(@RequestBody SecurityUserRequest request) {
        return ResponseEntity.ok(userService.createSecurityUser(request));
    }

    // Reports
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(reportService.getDashboardStats());
    }

    @GetMapping("/reports/visitors")
    public ResponseEntity<List<VisitorHistory>> getVisitorReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(reportService.getVisitorReport(startDate, endDate, tenantId));
    }

    @GetMapping("/reports/vehicles")
    public ResponseEntity<List<VehicleHistory>> getVehicleReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(reportService.getVehicleReport(startDate, endDate, tenantId));
    }
}
