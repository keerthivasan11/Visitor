package com.smartsecurity.system.controller;

import com.smartsecurity.system.dto.TenantResponse;
import com.smartsecurity.system.dto.VehicleRequest;
import com.smartsecurity.system.dto.VisitorRequest;
import com.smartsecurity.system.entity.Vehicle;
import com.smartsecurity.system.enums.UserType;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.security.JwtAuthenticationFilter;
import com.smartsecurity.system.entity.Visitor;
import com.smartsecurity.system.service.TenantService;
import com.smartsecurity.system.service.VehicleService;
import com.smartsecurity.system.service.VisitorService;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.entity.VisitorHistory;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityController {

    private final VisitorService visitorService;
    private final VehicleService vehicleService;
    private final TenantService tenantService;
    private final UserRepository userRepository;

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @GetMapping("/visitors/today")
    public ResponseEntity<List<Visitor>> getTodayVisitors() {
        return ResponseEntity.ok(visitorService.getVisitorsForDate(LocalDate.now()));
    }

    @GetMapping("/visitors/checked-in")
    public ResponseEntity<List<Visitor>> getCheckedInVisitors() {
        return ResponseEntity.ok(visitorService.getCheckedInVisitors());
    }

    @GetMapping("/visitors/checked-out")
    public ResponseEntity<List<Visitor>> getCheckedOutVisitors() {
        return ResponseEntity.ok(visitorService.getCheckedOutVisitors());
    }

    @PostMapping("/visitors/walk-in")
    public ResponseEntity<Visitor> addWalkIn(HttpServletRequest httpRequest,
            @Valid @RequestBody VisitorRequest request) {
        User user1 = JwtAuthenticationFilter.getCurrentUser();
        User user = userRepository.findById(user1.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        request.setCreatedByUserId(user.getId());
        return ResponseEntity.ok(visitorService.addWalkInVisitor(request));
    }

    @PostMapping("/visitors/{id}/check-in")
    public ResponseEntity<Visitor> checkIn(HttpServletRequest httpRequest, @PathVariable Long id) {

        return ResponseEntity.ok(visitorService.checkIn(id));
    }

    @PostMapping("/visitors/{id}/check-out")
    public ResponseEntity<Visitor> checkOut(HttpServletRequest httpRequest, @PathVariable Long id) {

        return ResponseEntity.ok(visitorService.checkOut(id));
    }

    @GetMapping("/visitors/{id}/history")
    public ResponseEntity<List<VisitorHistory>> getVisitorHistory(@PathVariable Long id) {
        return ResponseEntity.ok(visitorService.getVisitorHistory(id));
    }

    // Vehicle Endpoints

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping("/vehicles/checked-in")
    public ResponseEntity<List<Vehicle>> getCheckedInVehicles() {
        return ResponseEntity.ok(vehicleService.getCheckedInVehicles());
    }

    @GetMapping("/vehicles/checked-out")
    public ResponseEntity<List<Vehicle>> getCheckedOutVehicles() {
        return ResponseEntity.ok(vehicleService.getCheckedOutVehicles());
    }

    @GetMapping("/vehicles/tenant")
    public ResponseEntity<List<Vehicle>> getVehiclesByTenant(@AuthenticationPrincipal User admin) {
        if (admin.getTenant() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(vehicleService.getVehiclesByTenant(admin.getTenant().getId()));
    }

    @PostMapping("/vehicles/entry")
    public ResponseEntity<Vehicle> vehicleEntry(HttpServletRequest httpRequest, @RequestBody VehicleRequest request) {
        // User user1 = JwtAuthenticationFilter.getCurrentUser();
        // User user = userRepository.findById(user1.getId())
        //         .orElseThrow(() -> new RuntimeException("User not found"));
        // request.setCreatedByUserId(user.getId());
        request.setUserType(UserType.SECURITY);
        return ResponseEntity.ok(vehicleService.checkInVehicle(request));
    }

    @PostMapping("/vehicles/{id}/check-in")
    public ResponseEntity<Vehicle> performCheckIn(HttpServletRequest httpRequest, @PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.performCheckIn(id));
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<Vehicle> updateVehicle(HttpServletRequest httpRequest, @PathVariable Long id,
            @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    @PostMapping("/vehicles/{id}/exit")
    public ResponseEntity<Vehicle> vehicleExit(HttpServletRequest httpRequest, @PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.checkOutVehicle(id));
    }

    @GetMapping("/vehicles/{id}/history")
    public ResponseEntity<List<VehicleHistory>> getVehicleHistory(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleHistory(id));
    }

    @GetMapping("/vehicles/{number}")
    public ResponseEntity<Vehicle> getVehicleDetails(@PathVariable String number) {
        return vehicleService.findByNumber(number)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Map<String, String>> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Vehicle deleted successfully");
        return ResponseEntity.ok(response);
    }

}
