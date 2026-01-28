package com.smartsecurity.system.controller;

import com.smartsecurity.system.dto.StaffRequest;
import com.smartsecurity.system.dto.TenantAdminRequest;
import com.smartsecurity.system.dto.TenantRequest;
import com.smartsecurity.system.dto.TenantResponse;

import com.smartsecurity.system.entity.Staff;
import com.smartsecurity.system.entity.StaffHistory;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.entity.VisitorHistory;

import com.smartsecurity.system.service.ReportService;
import com.smartsecurity.system.service.TenantService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final TenantService tenantService;
    private final ReportService reportService;

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
            @PathVariable Integer adminId,
            @RequestBody TenantAdminRequest request) {
        return ResponseEntity.ok(tenantService.updateTenantAdmin(adminId, request));
    }

    @DeleteMapping("/tenants/admins/{adminId}")
    public ResponseEntity<Map<String, String>> deleteTenantAdmin(@PathVariable Integer adminId) {
        log.info("DELETE request received for adminId: {}", adminId);
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

    // Reports
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(reportService.getDashboardStats());
    }

    @GetMapping("/reports/visitors")
    public ResponseEntity<List<VisitorHistory>> getVisitorReport(@AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(reportService.getVisitorReport(startDate, endDate, tenantId));
    }

    @GetMapping("/reports/vehicles")
    public ResponseEntity<List<VehicleHistory>> getVehicleReport(@AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(reportService.getVehicleReport(startDate, endDate, tenantId));
    }

    @GetMapping("/reports/staff")
    public ResponseEntity<List<StaffHistory>> getStaffReport(@AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getStaffReport(startDate, endDate));
    }

    // staff

    @GetMapping(value = "/getAllStaff")
    public ResponseEntity<List<Staff>> getAllStaff() {
        return ResponseEntity.ok(tenantService.getAllStaff());
    }

    @PostMapping(value = "/addStaff")
    public ResponseEntity<Staff> addStaff(@RequestBody StaffRequest staffRequest) {
        return ResponseEntity.ok(tenantService.addStaff(staffRequest));
    }

    @PutMapping("/updateStaff/{id}")
    public ResponseEntity<Staff> updateStaff(@PathVariable Integer id, @RequestBody StaffRequest staffRequest) {
        return ResponseEntity.ok(tenantService.updateStaff(id, staffRequest));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Map<String, String>> deleteStaff(@PathVariable Integer id) {
        tenantService.deleteStaff(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Staff deleted successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff/{id}/check-in")
    public ResponseEntity<Staff> checkIn(HttpServletRequest httpRequest, @PathVariable Integer id) {
        return ResponseEntity.ok(tenantService.checkIn(id));
    }

    @PostMapping("/staff/{id}/check-out")
    public ResponseEntity<Staff> checkOut(HttpServletRequest httpRequest, @PathVariable Integer id) {
        return ResponseEntity.ok(tenantService.checkOut(id));
    }
}
