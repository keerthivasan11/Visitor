package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.AdminResponse;
import com.smartsecurity.system.dto.StaffRequest;
import com.smartsecurity.system.dto.TenantAdminRequest;
import com.smartsecurity.system.dto.TenantAdminResponse;
import com.smartsecurity.system.dto.TenantRequest;
import com.smartsecurity.system.dto.TenantResponse;

import com.smartsecurity.system.entity.Staff;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;

import com.smartsecurity.system.entity.StaffHistory;
import com.smartsecurity.system.enums.Role;
import com.smartsecurity.system.enums.UserStatus;
import com.smartsecurity.system.enums.VehicleStatus;
import com.smartsecurity.system.enums.VisitStatus;
import com.smartsecurity.system.exception.ResourceNotFoundException;
import com.smartsecurity.system.repository.FileRepository;
import com.smartsecurity.system.repository.StaffHistoryRepository;
import com.smartsecurity.system.repository.StaffRepository;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.repository.VehicleHistoryRepository;
import com.smartsecurity.system.repository.VehicleRepository;
import com.smartsecurity.system.repository.VisitorHistoryRepository;
import com.smartsecurity.system.repository.VisitorRepository;
import com.smartsecurity.system.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

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
    private final VisitorRepository visitorRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final VehicleHistoryRepository vehicleHistoryRepository;
    private final FileRepository fileRepository;
    private final VisitorHistoryRepository visitorHistoryRepository;

    // public List<TenantResponse> getAllTenants() {
    // return tenantRepository.findAll().stream()
    // .map(tenant -> {
    // List<User> admins = userRepository.findByTenantId(tenant.getId());
    // return TenantResponse.builder()
    // .id(tenant.getId())
    // .companyName(tenant.getCompanyName())
    // .companyCode(tenant.getCompanyCode())
    // .floorNumber(tenant.getFloorNumber())
    // .officeNumber(tenant.getOfficeNumber())
    // .block(tenant.getBlock())
    // .status(tenant.getStatus())
    // .admins(admins)
    // .build();
    // })
    // .toList();
    // }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {

        List<Tenant> tenants = tenantRepository.findAll();

        // Collect all tenant IDs
        List<Long> tenantIds = tenants.stream()
                .map(Tenant::getId)
                .collect(Collectors.toList());

        // Fetch all users for these tenants in ONE query
        List<User> allAdmins = userRepository.findByTenant_IdIn(tenantIds);

        // Group admins by tenant ID
        Map<Long, List<User>> adminsByTenant = allAdmins.stream()
                .collect(Collectors.groupingBy(user -> user.getTenant().getId()));

        // Build response
        return tenants.stream()
                .map(tenant -> {

                    List<User> admins = adminsByTenant
                            .getOrDefault(tenant.getId(), new ArrayList<>());

                    Set<AdminResponse> adminResponses = admins.stream()
                            .map(user -> AdminResponse.builder()
                                    .id(user.getId())
                                    .fullName(user.getFullName())
                                    .email(user.getEmail())
                                    .build())
                            .collect(Collectors.toSet());

                    return TenantResponse.builder()
                            .id(tenant.getId())
                            .companyName(tenant.getCompanyName())
                            .companyCode(tenant.getCompanyCode())
                            .floorNumber(tenant.getFloorNumber())
                            .officeNumber(tenant.getOfficeNumber())
                            .block(tenant.getBlock())
                            .status(tenant.getStatus())
                            .admins(adminResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Tenant createTenant(TenantRequest request) {
        Tenant tenant = new Tenant();
        tenant.setCompanyName(request.getCompanyName());
        tenant.setCompanyCode(request.getCompanyCode());
        tenant.setFloorNumber(request.getFloorNumber());
        tenant.setOfficeNumber(request.getOfficeNumber());
        tenant.setBlock(request.getBlock());
        tenant.setStatus(request.getStatus());
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

    @Transactional
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
                .createdDate(LocalDateTime.now())
                .build();
        return userRepository.save(admin);
    }

    public List<AdminResponse> getTenantAdmins(Long tenantId) {

        List<User> users = userRepository.findByTenant_Id(tenantId);

        return users.stream()
                .map(user -> AdminResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .mobileNumber(user.getMobileNumber())
                        .idProof(user.getIdProof())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTenantAdmin(Long adminId) {
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

    @Transactional
    public TenantAdminResponse updateTenantAdmin(Long adminId, TenantAdminRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Verify the user is actually a tenant admin
        if (admin.getRole() != Role.TENANT_ADMIN) {
            throw new RuntimeException("User is not a tenant admin");
        }

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

        return new TenantAdminResponse(
                updatedAdmin.getId(),
                updatedAdmin.getFullName(),
                updatedAdmin.getEmail(),
                updatedAdmin.getMobileNumber(),
                updatedAdmin.getStatus());
    }

    @Transactional
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
        if (request.getBlock() != null) {
            tenant.setBlock(request.getBlock());
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

        long activeVehicleCount = vehicleRepository.countByTenant_IdAndCheckOutTimeIsNull(id);

        if (activeVehicleCount > 0) {
            throw new RuntimeException("Active vehicles exist.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        visitorRepository.forceCheckoutVisitors(id, VisitStatus.CHECKED_OUT, now);
        visitorHistoryRepository.forceCheckoutVisitorHistory(id, VisitStatus.CHECKED_OUT, now);
        // visitorRepository.updateStatusByTenantId(id, VisitStatus.CHECKED_OUT);
        // visitorHistoryRepository.updateStatusByTenantId(id, VisitStatus.CHECKED_OUT);
        vehicleRepository.updateStatusByTenantId(id, VehicleStatus.CHECKED_OUT);
        vehicleHistoryRepository.updateStatusByTenantId(id, VehicleStatus.CHECKED_OUT);
        userRepository.updateStatusByTenantId(id, UserStatus.INACTIVE);
        fileRepository.updateStatusByTenantId(id, UserStatus.INACTIVE);
        tenant.setStatus(UserStatus.INACTIVE);

    }

    // staff

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    @Transactional
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
                .createdAt(LocalDateTime.now())
                .build();

        Staff savedStaff = staffRepository.save(staff);
        return savedStaff;
    }

    @Transactional
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

    @Transactional
    public Staff checkIn(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (staff.getStatus() == VisitStatus.CHECKED_IN) {
            throw new IllegalStateException("Staff already checked in");
        }
        LocalDateTime istNow = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        staff.setStatus(VisitStatus.CHECKED_IN);
        staff.setCheckInTime(istNow);
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

    @Transactional
    public Staff checkOut(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        LocalDateTime istNow = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        if (staff.getStatus() != VisitStatus.CHECKED_IN) {
            throw new RuntimeException("Staff is not checked in");
        }

        staff.setStatus(VisitStatus.CHECKED_OUT);
        staff.setCheckOutTime(istNow);

        staffHistoryRepository.findByStaffIdAndCheckOutTimeIsNull(staffId)
                .ifPresent(history -> {
                    history.setStatus(staff.getStatus());
                    history.setCheckOutTime(staff.getCheckOutTime());
                    staffHistoryRepository.save(history);
                });

        return staffRepository.save(staff);
    }

    public List<Staff> getCheckedInStaff() {
        // Get staff that are PENDING or CHECKED_IN (for Tab 1)
        return staffRepository.findAll().stream()
                .filter(v -> v.getStatus() == VisitStatus.PENDING ||
                        v.getStatus() == VisitStatus.CHECKED_IN)
                .toList();
    }

    public List<Staff> getCheckedOutStaff() {
        // Get staff that are CHECKED_OUT (for Tab 2)
        return staffRepository.findAll().stream()
                .filter(v -> v.getStatus() == VisitStatus.CHECKED_OUT)
                .toList();
    }

    public Page<StaffHistory> getStaffHistory(Long vehicleId, int page, int size, LocalDateTime start,
            LocalDateTime end) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkInTime").descending());
        return staffHistoryRepository.findByStaffIdWithFilters(vehicleId, start, end, pageable);
    }
}
