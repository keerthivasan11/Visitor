package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.TenantAdminRequest;
import com.smartsecurity.system.dto.TenantRequest;
import com.smartsecurity.system.dto.TenantResponse;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.enums.Role;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(tenant -> {
                    List<User> admins = userRepository.findByTenantId(tenant.getId());
                    return TenantResponse.builder()
                            .id(tenant.getId())
                            .companyName(tenant.getCompanyName())
                            .companyCode(tenant.getCompanyCode())
                            .floorNumber(tenant.getFloorNumber())
                            .officeNumber(tenant.getOfficeNumber())
                            .status(tenant.getStatus())
                            .admins(admins)
                            .build();
                })
                .toList();
    }

    // public List<Tenant> getAllTenants() {
    // return tenantRepository.findAll();
    // }

    public Tenant createTenant(TenantRequest request) {
        Tenant tenant = Tenant.builder()
                .companyName(request.getCompanyName())
                .companyCode(request.getCompanyCode())
                .floorNumber(request.getFloorNumber())
                .officeNumber(request.getOfficeNumber())
                .status(request.getStatus())
                .build();
        return tenantRepository.save(tenant);
    }

    public User addTenantAdmin(Long tenantId, TenantAdminRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        User admin = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .idProof(request.getIdProof())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.TENANT_ADMIN)
                .status(request.getStatus())
                .tenant(tenant)
                .build();
        return userRepository.save(admin);
    }

    public List<User> getTenantAdmins(Long tenantId) {
        // Tenant tenant = tenantRepository.findById(tenantId)
        //         .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return userRepository.findByTenantId(tenantId);
    }

    public void deleteTenantAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Verify the user is actually a tenant admin
        if (admin.getRole() != Role.TENANT_ADMIN) {
            throw new RuntimeException("User is not a tenant admin");
        }

        log.info("Deleting tenant admin: {} (ID: {})", admin.getEmail(), adminId);

        userRepository.delete(admin);

        log.info("Successfully deleted tenant admin: {}", admin.getEmail());
    }

    public User updateTenantAdmin(Long adminId, TenantAdminRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Verify the user is actually a tenant admin
        if (admin.getRole() != Role.TENANT_ADMIN) {
            throw new RuntimeException("User is not a tenant admin");
        }

        log.info("Updating tenant admin: {} (ID: {})", admin.getEmail(), adminId);

        // Update fields if provided
        if (request.getFullName() != null) {
            admin.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            admin.setEmail(request.getEmail());
        }
        if (request.getMobileNumber() != null) {
            admin.setMobileNumber(request.getMobileNumber());
        }
        if (request.getIdProof() != null) {
            admin.setIdProof(request.getIdProof());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getStatus() != null) {
            admin.setStatus(request.getStatus());
        }

        User updatedAdmin = userRepository.save(admin);

        log.info("Successfully updated tenant admin: {}", admin.getEmail());

        return updatedAdmin;
    }

    public Tenant updateTenant(Long id, TenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (request.getCompanyName() != null) {
            tenant.setCompanyName(request.getCompanyName());
        }
        if (request.getCompanyCode() != null) {
            tenant.setCompanyCode(request.getCompanyCode());
        }
        if (request.getFloorNumber() != null) {
            tenant.setFloorNumber(request.getFloorNumber());
        }
        if (request.getOfficeNumber() != null) {
            tenant.setOfficeNumber(request.getOfficeNumber());
        }
        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
        }

        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Check for active vehicles (vehicles that haven't checked out)
        long activeVehicleCount = vehicleRepository.countByTenant_IdAndCheckOutTimeIsNull(tenant.getId());

        if (activeVehicleCount > 0) {
            log.warn("Attempted to delete tenant {} with {} active vehicles",
                    tenant.getCompanyName(), activeVehicleCount);
            throw new RuntimeException(
                    String.format(
                            "Cannot delete tenant '%s'. There are %d active vehicle(s) that haven't checked out yet. " +
                                    "Please ensure all vehicles are checked out before deleting the tenant.",
                            tenant.getCompanyName(), activeVehicleCount));
        }

        log.info("Deleting tenant: {} (ID: {})", tenant.getCompanyName(), id);

        // Get count for logging (admins only, vehicles are decoupled)
        int adminCount = tenant.getAdmins() != null ? tenant.getAdmins().size() : 0;

        // Delete tenant (cascade will handle users)
        tenantRepository.delete(tenant);

        log.info("Successfully deleted tenant: {} along with {} admin(s)",
                tenant.getCompanyName(), adminCount);
    }
}
