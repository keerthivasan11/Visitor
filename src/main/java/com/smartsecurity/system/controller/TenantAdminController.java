package com.smartsecurity.system.controller;

import com.smartsecurity.system.dto.ApprovalRequest;
import com.smartsecurity.system.dto.VisitorRequest;
import com.smartsecurity.system.dto.VehicleRequest;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.entity.Visitor;
import com.smartsecurity.system.entity.Vehicle;
import com.smartsecurity.system.enums.UserType;
import com.smartsecurity.system.service.VisitorService;
import com.smartsecurity.system.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant-admin")
@RequiredArgsConstructor
public class TenantAdminController {

    private final VisitorService visitorService;
    private final VehicleService vehicleService;

    @PostMapping("/visitors/schedule")
    public ResponseEntity<Visitor> scheduleVisitor(@RequestBody VisitorRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(visitorService.scheduleVisitor(request, admin));
    }

    @PutMapping("/visitors/{id}")
    public ResponseEntity<Visitor> updateScheduledVisitor(
            @PathVariable Long id,
            @RequestBody VisitorRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(visitorService.updateScheduledVisitor(id, request, admin));
    }

    @GetMapping("/visitors")
    public ResponseEntity<List<Visitor>> getAllVisitors(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(visitorService.getAllVisitorsForTenant(admin.getTenant().getId(), admin));
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<List<Visitor>> getPendingApprovals(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(visitorService.getPendingApprovalsForTenant(admin));
    }

    @GetMapping("/visitors/today")
    public ResponseEntity<List<Visitor>> getTodayVisitors(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(visitorService.getTodayVisitorsForTenant(admin.getTenant().getId()));
    }

    @DeleteMapping("/visitors/{id}")
    public ResponseEntity<Map<String, String>> deleteVisitor(
            @PathVariable Long id
           ) {
        visitorService.deleteVisitor(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Visitor deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/approvals/{id}")
    public ResponseEntity<Visitor> approveOrReject(@PathVariable Long id, @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(visitorService.approveOrReject(id, request, admin));
    }

    @PutMapping("/tenantsVehicles/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @RequestBody VehicleRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(vehicleService.updateTenantVehicle(id, request, admin.getTenant().getId()));
    }

    // Vehicle Endpoints for Tenant Admin
    @PostMapping("/tenantsVehicles/entry")
    public ResponseEntity<Vehicle> vehicleEntry(@RequestBody VehicleRequest request,
            @AuthenticationPrincipal User admin) {
        request.setCreatedByUserId(admin.getId());
        request.setTenantId(admin.getTenant().getId());
        request.setCompany(admin.getTenant().getCompanyName());
        request.setUserType(UserType.TENANT);
        return ResponseEntity.ok(vehicleService.checkInVehicle(request));
    }

    @DeleteMapping("/tenantsVehicles/{id}")
    public ResponseEntity<Map<String, String>> deleteVehicle(@PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        vehicleService.deleteTenantVehicle(id, admin.getTenant().getId());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Vehicle deleted successfully");
        return ResponseEntity.ok(response);
    }
}
