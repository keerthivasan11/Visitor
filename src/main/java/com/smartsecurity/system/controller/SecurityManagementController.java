package com.smartsecurity.system.controller;

import com.smartsecurity.system.dto.SecurityRequest;
import com.smartsecurity.system.entity.Security;
import com.smartsecurity.system.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/security-personnel")
@RequiredArgsConstructor
public class SecurityManagementController {

    private final SecurityService securityService;

    @GetMapping("/security/all")
    public ResponseEntity<List<Security>> getAllSecurity() {
        return ResponseEntity.ok(securityService.getAllSecurity());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Security> getSecurityById(@PathVariable Long id) {
        return securityService.getSecurityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Security>> getActiveSecurity() {
        return ResponseEntity.ok(securityService.getActiveSecurityPersonnel());
    }

    @PostMapping("/security/add")
    public ResponseEntity<Security> createSecurity(@RequestBody SecurityRequest request) {
        return ResponseEntity.ok(securityService.createSecurity(request));
    }

    @PutMapping("/security/{id}")
    public ResponseEntity<Security> updateSecurity(@PathVariable Long id, @RequestBody SecurityRequest request) {
        return ResponseEntity.ok(securityService.updateSecurity(id, request));
    }

    @DeleteMapping("/security/{id}")
    public ResponseEntity<Map<String, String>> deleteSecurity(@PathVariable Long id) {
        securityService.deleteSecurity(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Security deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/security/{id}/deactivate")
    public ResponseEntity<Security> deactivateSecurity(@PathVariable Long id) {
        return ResponseEntity.ok(securityService.deactivateSecurity(id));
    }

    @PatchMapping("/security/{id}/activate")
    public ResponseEntity<Security> activateSecurity(@PathVariable Long id) {
        return ResponseEntity.ok(securityService.activateSecurity(id));
    }
}
