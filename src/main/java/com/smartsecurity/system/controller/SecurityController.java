package com.smartsecurity.system.controller;

import com.smartsecurity.system.dto.TenantResponse;
import com.smartsecurity.system.dto.VehicleRequest;
import com.smartsecurity.system.dto.VehicleResponse;
import com.smartsecurity.system.dto.VisitorCheckInResponse;
import com.smartsecurity.system.dto.VisitorRequest;
import com.smartsecurity.system.dto.VisitorResponse;
import com.smartsecurity.system.entity.Vehicle;
import com.smartsecurity.system.enums.UserType;
import com.smartsecurity.system.repository.FileRepository;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.security.JwtAuthenticationFilter;
import com.smartsecurity.system.entity.Visitor;
import com.smartsecurity.system.service.TenantService;
import com.smartsecurity.system.service.VehicleService;
import com.smartsecurity.system.service.VisitorService;
import com.smartsecurity.system.entity.File;
import com.smartsecurity.system.entity.Staff;
import com.smartsecurity.system.entity.StaffHistory;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.entity.VisitorHistory;
import java.util.Base64;
import org.springframework.http.MediaType;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityController {

    private final VisitorService visitorService;
    private final VehicleService vehicleService;
    private final TenantService tenantService;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    @GetMapping("/tenants/all")
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
    public ResponseEntity<VisitorResponse> addWalkIn(HttpServletRequest httpRequest,
            @Valid @RequestBody VisitorRequest request) {
        User user1 = JwtAuthenticationFilter.getCurrentUser();
        User user = userRepository.findById(user1.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        request.setCreatedByUserId(user.getId());
        return ResponseEntity.ok(visitorService.addWalkInVisitor(request));
    }

    @PostMapping("/visitors/{id}/check-in")
    public ResponseEntity<VisitorCheckInResponse> checkIn(HttpServletRequest httpRequest, @PathVariable Long id) {
        return ResponseEntity.ok(visitorService.checkIn(id));
    }

    @PostMapping("/visitors/{id}/check-out")
    public ResponseEntity<Visitor> checkOut(HttpServletRequest httpRequest, @PathVariable Long id) {

        return ResponseEntity.ok(visitorService.checkOut(id));
    }

    @GetMapping("/visitors/history")
    public ResponseEntity<?> getVisitorHistory(
            @RequestParam(required = false) Long visitorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            }
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid date format. Use YYYY-MM-DD");
        }
        Page<VisitorHistory> historyPage = visitorService.getVisitorHistory(visitorId, page, size, start, end);
        if (historyPage.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "no history found");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(historyPage);
    }

    // Vehicle Endpoints

    @GetMapping("/vehicles/all")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping("/vehicles/checked-in")
    public ResponseEntity<List<VehicleResponse>> getCheckedInVehicles() {
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
    public ResponseEntity<VehicleResponse> vehicleEntry(@RequestBody VehicleRequest request,
            @AuthenticationPrincipal User currentUser) {
        request.setUserType(UserType.SECURITY);
        return ResponseEntity.ok(vehicleService.checkInVehicle(request, currentUser));
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

    @GetMapping("/vehicles/history")
    public ResponseEntity<?> getVehicleHistory(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDateTime start = null;
        LocalDateTime end = null;

        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            }
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid date format. Use YYYY-MM-DD");
        }

        Page<VehicleHistory> historyPage = vehicleService.getVehicleHistory(vehicleId, page, size, start, end);

        if (historyPage.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "no history found"));
        }

        return ResponseEntity.ok(historyPage);
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

    // staff

    @PostMapping("/staff/{id}/check-in")
    public ResponseEntity<Staff> checkIn(HttpServletRequest httpRequest, @PathVariable Integer id) {
        return ResponseEntity.ok(tenantService.checkIn(id));
    }

    @PostMapping("/staff/{id}/check-out")
    public ResponseEntity<Staff> checkOut(HttpServletRequest httpRequest, @PathVariable Integer id) {
        return ResponseEntity.ok(tenantService.checkOut(id));
    }

    @GetMapping(value = "/getAllStaff")
    public ResponseEntity<List<Staff>> getAllStaff() {
        return ResponseEntity.ok(tenantService.getAllStaff());
    }

    @GetMapping("/staff/checked-in")
    public ResponseEntity<List<Staff>> getCheckedInStaff() {
        return ResponseEntity.ok(tenantService.getCheckedInStaff());
    }

    @GetMapping("/staff/checked-out")
    public ResponseEntity<List<Staff>> getCheckedOutStaff() {
        return ResponseEntity.ok(tenantService.getCheckedOutStaff());
    }

    @GetMapping("/staff/history")
    public ResponseEntity<?> getStaffHistory(
            @RequestParam(required = false) Long staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            }
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid date format. Use YYYY-MM-DD");
        }
        Page<StaffHistory> historyPage = tenantService.getStaffHistory(staffId, page, size, start, end);
        if (historyPage.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "no history found"));
        }
        return ResponseEntity.ok(historyPage);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {

        File file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        byte[] imageBytes = Base64.getDecoder()
                .decode(file.getFileData());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    @GetMapping("/visitors/{id}/attachment/image")
    public ResponseEntity<byte[]> getAttachmentImage(@PathVariable Long id) {

        File file = visitorService.getVisitorFile(id);

        byte[] imageBytes = Base64.getDecoder()
                .decode(file.getFileData());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getFileType()))
                .body(imageBytes);
    }

}
