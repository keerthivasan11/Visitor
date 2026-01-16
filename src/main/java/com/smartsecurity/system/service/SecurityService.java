package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.SecurityRequest;
import com.smartsecurity.system.entity.Security;

import com.smartsecurity.system.enums.UserStatus;
import com.smartsecurity.system.repository.SecurityRepository;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final SecurityRepository securityRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Security> getAllSecurity() {
        return securityRepository.findAll();
    }

    public Optional<Security> getSecurityById(Long id) {
        return securityRepository.findById(id);
    }

    public List<Security> getActiveSecurityPersonnel() {
        return securityRepository.findByStatus(UserStatus.ACTIVE);
    }

    public Security createSecurity(SecurityRequest request) {
        // Check if email already exists in security table
        if (securityRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists in security table");
        }

        // Check if email already exists in users table
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists in users table");
        }

        // Check if mobile number already exists
        if (request.getMobileNumber() != null &&
                securityRepository.findByMobileNumber(request.getMobileNumber()).isPresent()) {
            throw new RuntimeException("Mobile number already exists");
        }

        // Encode password once
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Create Security entity
        Security security = Security.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .password(encodedPassword)
                .idProof(request.getIdProof())
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
                .build();

        Security savedSecurity = securityRepository.save(security);

        // Also create User entity for authentication with SECURITY_USER role
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .password(encodedPassword)
                .role(Role.SECURITY_USER)
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
                .tenant(null) // Security users don't belong to a tenant
                .build();

        userRepository.save(user);

        return savedSecurity;
    }

    public Security updateSecurity(Long id, SecurityRequest request) {
        Security security = securityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Security personnel not found"));

        if (request.getFullName() != null) {
            security.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(security.getEmail())) {
            if (securityRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists");
            }
            security.setEmail(request.getEmail());
        }
        if (request.getMobileNumber() != null) {
            security.setMobileNumber(request.getMobileNumber());
        }
        if (request.getPassword() != null) {
            security.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getIdProof() != null) {
            security.setIdProof(request.getIdProof());
        }
        if (request.getStatus() != null) {
            security.setStatus(request.getStatus());
        }

        return securityRepository.save(security);
    }

    public void deleteSecurity(Long id) {
        Security security = securityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Security personnel not found"));
        securityRepository.delete(security);
    }

    public Security deactivateSecurity(Long id) {
        Security security = securityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Security personnel not found"));
        security.setStatus(UserStatus.INACTIVE);
        return securityRepository.save(security);
    }

    public Security activateSecurity(Long id) {
        Security security = securityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Security personnel not found"));
        security.setStatus(UserStatus.ACTIVE);
        return securityRepository.save(security);
    }
}
