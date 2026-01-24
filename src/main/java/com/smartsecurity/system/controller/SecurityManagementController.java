package com.smartsecurity.system.controller;

import com.smartsecurity.system.dto.SecurityRequest;
import com.smartsecurity.system.entity.Security;
import com.smartsecurity.system.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/super-admin/security-personnel")
@RequiredArgsConstructor
public class SecurityManagementController {

    private final SecurityService securityService;

    @GetMapping
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

    @PostMapping
    public ResponseEntity<Security> createSecurity(@RequestBody SecurityRequest request) {
        return ResponseEntity.ok(securityService.createSecurity(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Security> updateSecurity(@PathVariable Long id, @RequestBody SecurityRequest request) {
        return ResponseEntity.ok(securityService.updateSecurity(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSecurity(@PathVariable Long id) {
        securityService.deleteSecurity(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Security> deactivateSecurity(@PathVariable Long id) {
        return ResponseEntity.ok(securityService.deactivateSecurity(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Security> activateSecurity(@PathVariable Long id) {
        return ResponseEntity.ok(securityService.activateSecurity(id));
    }
}
