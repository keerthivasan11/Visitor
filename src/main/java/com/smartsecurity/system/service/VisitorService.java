package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.ApprovalRequest;
import com.smartsecurity.system.dto.VisitorRequest;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.entity.Visitor;
import com.smartsecurity.system.enums.VisitStatus;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.repository.VisitorHistoryRepository;
import com.smartsecurity.system.repository.VisitorRepository;
import com.smartsecurity.system.entity.VisitorHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final VisitorHistoryRepository visitorHistoryRepository;

    public List<Visitor> getVisitorsForDate(LocalDate date) {
        return visitorRepository.findByVisitDate(date);
    }

    public List<Visitor> getCheckedInVisitors() {
        // Get visitors who have checked in but not checked out yet
        return visitorRepository.findAll().stream()
                .filter(v -> v.getCheckInTime() != null && v.getCheckOutTime() == null)
                .toList();
    }

    public List<Visitor> getCheckedOutVisitors() {
        // Get visitors who have checked out
        return visitorRepository.findAll().stream()
                .filter(v -> v.getCheckOutTime() != null)
                .toList();
    }

    public List<Visitor> getPendingApprovalsForTenant(Long tenantId) {
        // Filter visitors where tenant ID matches and status is PENDING
        return visitorRepository.findAll().stream()
                .filter(v -> v.getStatus() == VisitStatus.PENDING)
                .filter(v -> v.getTenant() != null && v.getTenant().getId().equals(tenantId))
                .toList();
    }

    public List<Visitor> getPendingApprovalsForTenant(User admin) {
        System.out.println(
                "=== Getting pending approvals for admin: " + admin.getId() + " - " + admin.getFullName() + " ===");
        List<Visitor> allVisitors = visitorRepository.findAll();
        System.out.println("Total visitors in database: " + allVisitors.size());

        // Filter: PENDING status and same tenant (shown to all admins of this tenant)
        List<Visitor> result = visitorRepository.findAll().stream()
                .filter(v -> v.getStatus() == VisitStatus.PENDING)
                .filter(v -> v.getTenant() != null && v.getTenant().getId().equals(admin.getTenant().getId()))
                .toList();

        System.out.println("Filtered result: " + result.size() + " visitors");
        for (Visitor v : result) {
            System.out.println("  - Visitor: " + v.getId() + " - " + v.getVisitorName() +
                    " (Assigned admins: " + (v.getAssignedAdmins() != null ? v.getAssignedAdmins().size() : 0)
                    + ")");
        }
        System.out.println("=== End ===");
        return result;
    }

    public List<Visitor> getTodayVisitorsForTenant(Long tenantId) {
        LocalDate today = LocalDate.now();
        return visitorRepository.findAll().stream()
                .filter(v -> v.getVisitDate().equals(today))
                .filter(v -> v.getTenant() != null && v.getTenant().getId().equals(tenantId))
                .toList();
    }

    public List<Visitor> getAllVisitorsForTenant(Long tenantId) {
        return visitorRepository.findAll().stream()
                .filter(v -> v.getTenant() != null && v.getTenant().getId().equals(tenantId))
                .toList();
    }

    @Transactional
    public Visitor scheduleVisitor(VisitorRequest request, User tenantAdmin) {
        try {
            System.out.println("=== Scheduling Visitor ===");
            List<Long> adminIds = request.getEffectiveAdminIds();
            System.out.println("Effective admin IDs to assign: " + adminIds);

            // Step 1: Create and save visitor first
            Visitor visitor = Visitor.builder()
                    .visitorName(request.getVisitorName())
                    .mobileNumber(request.getMobileNumber())
                    .visitType(request.getVisitType())
                    .visitDate(request.getVisitDate())
                    .expectedTime(request.getExpectedTime())
                    .employeeToMeet(request.getEmployeeToMeet())
                    .status(VisitStatus.APPROVED) // Auto-approved if added by tenant admin
                    .approvedBy(tenantAdmin)
                    .tenant(tenantAdmin.getTenant())
                    .assignedAdmins(new HashSet<>())
                    .build();

            visitor = visitorRepository.save(visitor);
            System.out.println("Step 1: Visitor scheduled with base ID: " + visitor.getId());

            // Step 2: Process assigned admins if provided
            if (!adminIds.isEmpty()) {
                System.out.println("Step 2: Processing " + adminIds.size() + " admin IDs");
                Set<User> adminsToAssign = new HashSet<>();
                for (Long adminId : adminIds) {
                    User admin = userRepository.findById(adminId)
                            .orElseThrow(() -> new RuntimeException("Assigned admin not found: " + adminId));

                    if (!admin.getTenant().getId().equals(tenantAdmin.getTenant().getId())) {
                        System.out.println("NOTE: Admin " + adminId + " belongs to different tenant");
                    }
                    adminsToAssign.add(admin);
                }
                visitor.setAssignedAdmins(adminsToAssign);
                visitor = visitorRepository.save(visitor);
                System.out.println("Step 2 completes: Scheduled visitor " + visitor.getId() + " now has "
                        + visitor.getAssignedAdmins().size() + " admins assigned.");
            }

            System.out.println("=== End scheduling flow ===");
            return visitor;
        } catch (Exception e) {
            System.err.println("Error scheduling visitor: " + e.getMessage());
            throw new RuntimeException("Failed to schedule visitor: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Visitor updateScheduledVisitor(Long visitorId, VisitorRequest request, User tenantAdmin) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        // Verify the visitor belongs to this tenant admin's tenant
        boolean belongsToTenant = visitor.getTenant() != null &&
                visitor.getTenant().getId().equals(tenantAdmin.getTenant().getId());

        if (!belongsToTenant) {
            throw new RuntimeException("You can only update visitors for your tenant");
        }

        // Only allow updates if visitor hasn't checked in yet
        if (visitor.getStatus() == VisitStatus.CHECKED_IN || visitor.getStatus() == VisitStatus.CHECKED_OUT) {
            throw new RuntimeException("Cannot update visitor who has already checked in");
        }

        // Update fields if provided
        if (request.getVisitorName() != null) {
            visitor.setVisitorName(request.getVisitorName());
        }
        if (request.getMobileNumber() != null) {
            visitor.setMobileNumber(request.getMobileNumber());
        }
        if (request.getVisitType() != null) {
            visitor.setVisitType(request.getVisitType());
        }
        if (request.getIdProof() != null) {
            visitor.setIdProof(request.getIdProof());
        }
        if (request.getVisitDate() != null) {
            visitor.setVisitDate(request.getVisitDate());
        }
        if (request.getExpectedTime() != null) {
            visitor.setExpectedTime(request.getExpectedTime());
        }

        if (request.getEmployeeToMeet() != null) {
            visitor.setEmployeeToMeet(request.getEmployeeToMeet());
        }

        return visitorRepository.save(visitor);
    }

    @Transactional
    public Visitor addWalkInVisitor(VisitorRequest request) {
        System.out.println("=== Creating Walk-in Visitor ===");
        List<Long> adminIds = request.getEffectiveAdminIds();
        System.out.println("Effective admin IDs to assign: " + adminIds);

        // Get the tenant
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        User createdByUser = null;
        if (request.getCreatedByUserId() != null) {
            createdByUser = userRepository.findById(request.getCreatedByUserId()).orElse(null);
        }

        // Step 1: Create and save visitor first to generate ID
        Visitor visitor = Visitor.builder()
                .visitorName(request.getVisitorName())
                .mobileNumber(request.getMobileNumber())
                .visitType(request.getVisitType())
                .idProof(request.getIdProof())
                .imageUrl(request.getImageUrl())
                .visitDate(LocalDate.now())
                .status(VisitStatus.PENDING)
                .tenant(tenant)
                .createdBy(createdByUser)
                .assignedAdmins(new HashSet<>()) // Initialize empty set
                .build();

        // Initial save
        visitor = visitorRepository.save(visitor);
        System.out.println("Step 1: Visitor created with base ID: " + visitor.getId());

        // Step 2: Add assigned admins and save again
        if (!adminIds.isEmpty()) {
            System.out.println("Step 2: Processing " + adminIds.size() + " admin IDs");
            Set<User> adminsToAssign = new HashSet<>();
            for (Long adminId : adminIds) {
                User admin = userRepository.findById(adminId)
                        .orElseThrow(() -> new RuntimeException("Assigned admin not found: " + adminId));

                // Optional: Check tenant match
                if (!admin.getTenant().getId().equals(tenant.getId())) {
                    System.out.println("NOTE: Admin " + adminId + " belongs to different tenant");
                }
                adminsToAssign.add(admin);
            }

            visitor.setAssignedAdmins(adminsToAssign);
            visitor = visitorRepository.save(visitor);
            System.out.println("Step 2 completes: Visitor " + visitor.getId() + " now has "
                    + visitor.getAssignedAdmins().size() + " admins assigned.");
        } else {
            System.out.println("Step 2: No admin IDs provided in request.");
        }

        System.out.println("=== End creation flow ===");
        return visitor;
    }

    @Transactional
    public Visitor approveOrReject(Long visitorId, ApprovalRequest request, User admin) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        if (visitor.getStatus() != VisitStatus.PENDING) {
            throw new RuntimeException("Already processed: " + visitor.getStatus());
        }

        // Check if visitor belongs to admin's tenant
        boolean belongsToTenant = visitor.getTenant() != null &&
                visitor.getTenant().getId().equals(admin.getTenant().getId());

        if (!belongsToTenant) {
            throw new RuntimeException("You can only approve/reject visitors for your tenant");
        }

        // Check if admin is assigned to this visitor (if assignment exists)
        if (visitor.getAssignedAdmins() != null && !visitor.getAssignedAdmins().isEmpty()) {
            boolean isAssigned = visitor.getAssignedAdmins().stream()
                    .anyMatch(a -> a.getId().equals(admin.getId()));

            if (!isAssigned) {
                throw new RuntimeException("You are not assigned to approve/reject this visitor");
            }
        }

        visitor.setStatus(request.getStatus());
        visitor.setApprovedBy(admin);
        visitor.setRejectionRemarks(request.getRemarks());
        visitor = visitorRepository.save(visitor);

        // Save to history table
        VisitorHistory historyEntry = VisitorHistory.builder()
                .visitor(visitor)
                .visitorName(visitor.getVisitorName())
                .mobileNumber(visitor.getMobileNumber())
                .visitType(visitor.getVisitType())
                .idProof(visitor.getIdProof())
                .imageUrl(visitor.getImageUrl())
                .employeeToMeet(visitor.getEmployeeToMeet())
                .status(visitor.getStatus())
                .visitDate(visitor.getVisitDate())
                .tenant(visitor.getTenant())
                .createdBy(visitor.getCreatedBy())
                .approvedBy(admin)
                .rejectionRemarks(visitor.getRejectionRemarks())
                .build();

        visitorHistoryRepository.save(historyEntry);

        return visitor;
    }

    public Visitor checkIn(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        if (visitor.getStatus() != VisitStatus.APPROVED) {
            throw new RuntimeException("Visitor not approved");
        }
        visitor.setStatus(VisitStatus.CHECKED_IN);
        visitor.setCheckInTime(LocalTime.now());
        visitor = visitorRepository.save(visitor);

        // Try to find an existing history entry from approval step
        Optional<VisitorHistory> existingHistory = visitorHistoryRepository.findByVisitor(visitor).stream()
                .filter(h -> h.getCheckInTime() == null && h.getStatus() == VisitStatus.APPROVED)
                .findFirst();

        if (existingHistory.isPresent()) {
            VisitorHistory history = existingHistory.get();
            history.setStatus(visitor.getStatus());
            history.setCheckInTime(visitor.getCheckInTime());
            visitorHistoryRepository.save(history);
        } else {
            // Create new history entry at check-in (e.g. for scheduled visitors)
            VisitorHistory historyEntry = VisitorHistory.builder()
                    .visitor(visitor)
                    .visitorName(visitor.getVisitorName())
                    .mobileNumber(visitor.getMobileNumber())
                    .visitType(visitor.getVisitType())
                    .idProof(visitor.getIdProof())
                    .imageUrl(visitor.getImageUrl())
                    .employeeToMeet(visitor.getEmployeeToMeet())
                    .status(visitor.getStatus())
                    .visitDate(visitor.getVisitDate())
                    .checkInTime(visitor.getCheckInTime())
                    .tenant(visitor.getTenant())
                    .createdBy(visitor.getCreatedBy())
                    .build();

            visitorHistoryRepository.save(historyEntry);
        }

        return visitor;
    }

    public Visitor checkOut(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        visitor.setStatus(VisitStatus.CHECKED_OUT);
        visitor.setCheckOutTime(LocalTime.now());

        // Update existing history entry
        visitorHistoryRepository.findByVisitorAndCheckOutTimeIsNull(visitor)
                .ifPresent(history -> {
                    history.setStatus(visitor.getStatus());
                    history.setCheckOutTime(visitor.getCheckOutTime());
                    visitorHistoryRepository.save(history);
                });

        return visitorRepository.save(visitor);
    }

    public List<VisitorHistory> getVisitorHistory(Long visitorId) {
        return visitorHistoryRepository.findByVisitorId(visitorId);
    }

    @Transactional
    public void deleteVisitor(Long visitorId, User tenantAdmin) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        // Verify the visitor belongs to this tenant admin's tenant
        boolean belongsToTenant = visitor.getTenant() != null &&
                visitor.getTenant().getId().equals(tenantAdmin.getTenant().getId());

        if (!belongsToTenant) {
            throw new RuntimeException("You can only delete visitors for your tenant");
        }

        visitorRepository.delete(visitor);
    }

    public List<Visitor> getAllVisitorsForTenant(Long tenantId, User admin) {
        // TEMPORARY: Show all visitors for tenant (removed assignedAdmins filter for
        // debugging)
        return visitorRepository.findAll().stream()
                .filter(v -> v.getTenant() != null && v.getTenant().getId().equals(tenantId))
                .toList();
    }
}
