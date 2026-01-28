package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.StaffRequest;
import com.smartsecurity.system.dto.TenantAdminRequest;
import com.smartsecurity.system.dto.TenantRequest;
import com.smartsecurity.system.dto.TenantResponse;

import com.smartsecurity.system.entity.Staff;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;

import com.smartsecurity.system.entity.StaffHistory;
import com.smartsecurity.system.enums.Role;
import com.smartsecurity.system.enums.VisitStatus;
import com.smartsecurity.system.exception.ResourceNotFoundException;

import com.smartsecurity.system.repository.StaffHistoryRepository;
import com.smartsecurity.system.repository.StaffRepository;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.repository.VehicleRepository;
import com.smartsecurity.system.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final StaffRepository staffRepository;
    private final StaffHistoryRepository staffHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    private final NotificationDispatcher notificationDispatcher;

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

    public Tenant createTenant(TenantRequest request) {
        Tenant tenant = Tenant.builder()
                .companyName(request.getCompanyName())
                .companyCode(request.getCompanyCode())
                .floorNumber(request.getFloorNumber())
                .officeNumber(request.getOfficeNumber())
                .status(request.getStatus())
                .build();
        Tenant savedTenant = tenantRepository.save(tenant);
        notifyTenantCreated(savedTenant);
        return savedTenant;
    }

    private void notifyTenantCreated(Tenant tenant) {
        notificationDispatcher.sendAsync(
                "ADMIN_FCM_TOKEN", // fetch from DB ideally
                "New Tenant Added",
                "Tenant " + tenant.getCompanyName() + " created successfully");
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
        // .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return userRepository.findByTenantId(tenantId);
    }

    @Transactional
    public void deleteTenantAdmin(Integer adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Verify the user is actually a tenant admin
        if (admin.getRole() != Role.TENANT_ADMIN) {
            throw new RuntimeException("User is not a tenant admin");
        }
        Tenant tenant = admin.getTenant();

        if (tenant != null) {
            tenant.getAdmins().remove(admin);
            admin.setTenant(null);
        }
        userRepository.delete(admin);
        userRepository.flush();
    }

    public User updateTenantAdmin(Integer adminId, TenantAdminRequest request) {
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

    // staff

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    public Staff addStaff(StaffRequest staffRequest) {
        User currentUser = JwtAuthenticationFilter.getCurrentUser();
        if (staffRequest.getMobileNumber() != null &&
                staffRepository.findByMobileNumber(staffRequest.getMobileNumber()).isPresent()) {
            throw new RuntimeException("Mobile number already exists");
        }

        Staff staff = Staff.builder()
                .employeeCode(staffRequest.getEmployeeCode())
                .address(staffRequest.getAddress())
                .name(staffRequest.getName())
                .mobileNumber(staffRequest.getMobileNumber())
                .idProof(staffRequest.getIdProof())
                .status(staffRequest.getStatus())
                .createdBy(currentUser.getId())
                // .updatedBy(staffRequest.getUpdatedBy())
                .createdAt(LocalDateTime.now())
                // .updatedAt(staffRequest.getUpdatedAt())
                .build();

        Staff savedStaff = staffRepository.save(staff);
        return savedStaff;
    }

    public Staff updateStaff(Integer id, StaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        if (request.getEmployeeCode() != null) {
            staff.setEmployeeCode(request.getEmployeeCode());
        }
        if (request.getAddress() != null) {
            staff.setAddress(request.getAddress());
        }
        if (request.getName() != null) {
            staff.setName(request.getName());
        }

        if (request.getMobileNumber() != null) {
            staff.setMobileNumber(request.getMobileNumber());
        }

        if (request.getIdProof() != null) {
            staff.setIdProof(request.getIdProof());
        }
        if (request.getStatus() != null) {
            staff.setStatus(request.getStatus());
        }

        return staffRepository.save(staff);
    }

    @Transactional
    public void deleteStaff(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        staffRepository.delete(staff);
    }

    public Staff checkIn(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        staff.setStatus(VisitStatus.CHECKED_IN);
        staff.setCheckInTime(LocalDateTime.now());
        staff = staffRepository.save(staff);

        StaffHistory staffHistory = StaffHistory.builder()
                .staffId(staff.getId())
                .employeeCode(staff.getEmployeeCode())
                .address(staff.getAddress())
                .name(staff.getName())
                .mobileNumber(staff.getMobileNumber())
                .idProof(staff.getIdProof())
                .status(staff.getStatus())
                .checkInTime(staff.getCheckInTime())
                .createdAt(LocalDateTime.now())
                .createdBy(staff.getCreatedBy())
                .build();

        staffHistoryRepository.save(staffHistory);

        return staff;
    }

    public Staff checkOut(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (staff.getStatus() != VisitStatus.CHECKED_IN) {
            throw new RuntimeException("Staff is not checked in");
        }

        staff.setStatus(VisitStatus.CHECKED_OUT);
        staff.setCheckOutTime(LocalDateTime.now());

        staffHistoryRepository.findByStaffIdAndCheckOutTimeIsNull(staffId)
                .ifPresent(history -> {
                    history.setStatus(staff.getStatus());
                    history.setCheckOutTime(staff.getCheckOutTime());
                    staffHistoryRepository.save(history);
                });

        return staffRepository.save(staff);
    }
}
