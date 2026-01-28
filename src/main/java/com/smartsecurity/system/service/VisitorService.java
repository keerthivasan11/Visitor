package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.ApprovalRequest;
import com.smartsecurity.system.dto.VisitorRequest;

import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.entity.Visitor;
import com.smartsecurity.system.enums.Role;
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
    private final NotificationDispatcher notificationDispatcher;

    public List<Visitor> getVisitorsForDate(LocalDate date) {
        return visitorRepository.findByVisitDate(date);
    }

    public List<Visitor> getCheckedInVisitors() {

        return visitorRepository.findAll().stream()
                .filter(v -> v.getCheckInTime() != null && v.getCheckOutTime() == null)
                .toList();
    }

    public List<Visitor> getCheckedOutVisitors() {

        return visitorRepository.findAll().stream()
                .filter(v -> v.getCheckOutTime() != null)
                .toList();
    }

    public List<Visitor> getPendingApprovalsForTenant(Long tenantId) {

        return visitorRepository.findAll().stream()
                .filter(v -> v.getStatus() == VisitStatus.PENDING)
                .filter(v -> v.getTenant() != null && v.getTenant().getId().equals(tenantId))
                .toList();
    }

    public List<Visitor> getPendingApprovalsForTenant(User admin) {

        return visitorRepository.findPendingForAdmin(
                VisitStatus.PENDING,
                admin.getTenant().getId(),
                admin.getId());
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
            List<Integer> adminIds = request.getEffectiveAdminIds();
            System.out.println("Effective admin IDs to assign: " + adminIds);

            Visitor visitor = Visitor.builder()
                    .visitorName(request.getVisitorName())
                    .mobileNumber(request.getMobileNumber())
                    .visitType(request.getVisitType())
                    .visitDate(request.getVisitDate())
                    .status(VisitStatus.APPROVED)
                    // .approvedBy(tenantAdmin.getId())
                    .createdBy(tenantAdmin.getId())
                    .tenant(tenantAdmin.getTenant())
                    // .assignedAdmins(new HashSet<>())
                    .build();

            visitor = visitorRepository.save(visitor);
            System.out.println("Step 1: Visitor scheduled with base ID: " + visitor.getId());

            // if (!adminIds.isEmpty()) {
            // System.out.println("Step 2: Processing " + adminIds.size() + " admin IDs");
            // Set<User> adminsToAssign = new HashSet<>();
            // for (Integer adminId : adminIds) {
            // User admin = userRepository.findById(adminId)
            // .orElseThrow(() -> new RuntimeException("Assigned admin not found: " +
            // adminId));

            // if (!admin.getTenant().getId().equals(tenantAdmin.getTenant().getId())) {
            // System.out.println("NOTE: Admin " + adminId + " belongs to different
            // tenant");
            // }
            // adminsToAssign.add(admin);
            // }
            // visitor.setAssignedAdmins(adminsToAssign);
            // visitor = visitorRepository.save(visitor);
            // System.out.println("Step 2 completes: Scheduled visitor " + visitor.getId() +
            // " now has "
            // + visitor.getAssignedAdmins().size() + " admins assigned.");
            // }
            visitor = visitorRepository.save(visitor);
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

        boolean belongsToTenant = visitor.getTenant() != null &&
                visitor.getTenant().getId().equals(tenantAdmin.getTenant().getId());

        if (!belongsToTenant) {
            throw new RuntimeException("You can only update visitors for your tenant");
        }

        if (visitor.getStatus() == VisitStatus.CHECKED_IN || visitor.getStatus() == VisitStatus.CHECKED_OUT) {
            throw new RuntimeException("Cannot update visitor who has already checked in");
        }

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

        return visitorRepository.save(visitor);
    }

    @Transactional
    public Visitor addWalkInVisitor(VisitorRequest request) {
        System.out.println("=== Creating Walk-in Visitor ===");
        List<Integer> adminIds = request.getEffectiveAdminIds();
        System.out.println("Effective admin IDs to assign: " + adminIds);

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Integer createdByUserId = request.getCreatedByUserId();

        Visitor visitor = Visitor.builder()
                .visitorName(request.getVisitorName())
                .mobileNumber(request.getMobileNumber())
                .visitType(request.getVisitType())
                .idProof(request.getIdProof())
                .imageUrl(request.getImageUrl())
                .visitDate(LocalDate.now())
                .status(VisitStatus.PENDING)
                .tenant(tenant)
                .createdBy(createdByUserId)
                .assignedAdmins(new HashSet<>()) // Initialize empty set
                .build();

        visitor = visitorRepository.save(visitor);
        System.out.println("Step 1: Visitor created with base ID: " + visitor.getId());

        if (!adminIds.isEmpty()) {
            System.out.println("Step 2: Processing " + adminIds.size() + " admin IDs");
            Set<User> adminsToAssign = new HashSet<>();
            for (Integer adminId : adminIds) {
                User admin = userRepository.findById(adminId)
                        .orElseThrow(() -> new RuntimeException("Assigned admin not found: " + adminId));

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
        // 🔔 PUSH NOTIFICATION (ASYNC)
        sendVisitorCreatedNotifications(visitor);
        return visitor;
    }

    private void sendVisitorCreatedNotifications(Visitor visitor) {
        System.out.println("Checking");
        if (visitor.getAssignedAdmins() == null ||
                visitor.getAssignedAdmins().isEmpty()) {
            return;
        }

        String title = "New Walk-in Visitor";
        String body = "Visitor " + visitor.getVisitorName()
                + " is waiting for approval.";

        for (User admin : visitor.getAssignedAdmins()) {

            String fcmToken = admin.getFcmToken();

            if (fcmToken == null || fcmToken.isBlank()) {
                continue;
            }

            notificationDispatcher.sendAsync(
                    fcmToken,
                    title,
                    body);
        }
    }

    @Transactional
    public Visitor approveOrReject(Long visitorId, ApprovalRequest request, User admin) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        if (visitor.getStatus() != VisitStatus.PENDING) {
            throw new RuntimeException("Already processed: " + visitor.getStatus());
        }

        boolean belongsToTenant = visitor.getTenant() != null &&
                visitor.getTenant().getId().equals(admin.getTenant().getId());

        if (!belongsToTenant) {
            throw new RuntimeException("You can only approve/reject visitors for your tenant");
        }

        if (visitor.getAssignedAdmins() != null && !visitor.getAssignedAdmins().isEmpty()) {
            boolean isAssigned = visitor.getAssignedAdmins().stream()
                    .anyMatch(a -> a.getId().equals(admin.getId()));

            if (!isAssigned) {
                throw new RuntimeException("You are not assigned to approve/reject this visitor");
            }
        }

        visitor.setStatus(request.getStatus());
        visitor.setApprovedBy(admin.getId());
        visitor.setRejectionRemarks(request.getRemarks());
        visitor = visitorRepository.save(visitor);

        VisitorHistory historyEntry = VisitorHistory.builder()
                .visitorId(visitor.getId())
                .visitorName(visitor.getVisitorName())
                .mobileNumber(visitor.getMobileNumber())
                .visitType(visitor.getVisitType())
                .idProof(visitor.getIdProof())
                .imageUrl(visitor.getImageUrl())
                .status(visitor.getStatus())
                .visitDate(visitor.getVisitDate())
                .tenant(visitor.getTenant())
                .createdBy(visitor.getCreatedBy())
                .approvedBy(admin.getId())
                .rejectionRemarks(visitor.getRejectionRemarks())
                .build();

        visitorHistoryRepository.save(historyEntry);

        // 🔔 ADD NOTIFICATION CALL HERE ⬇⬇⬇
        sendSecurityCreatedNotifications(visitor);
        return visitor;
    }

    private void sendSecurityCreatedNotifications(Visitor visitor) {
        System.out.println("Checking");

        List<User> security = userRepository.findByRole(Role.SECURITY_USER);
        String approvedByName = "Admin";
        Integer approvedById = visitor.getApprovedBy();

        if (approvedById != null) {
            approvedByName = userRepository
                    .findById(approvedById) // Integer → Long
                    .map(User::getFullName)
                    .orElse("Admin");
        }
        String title = "Visitor Approved By " + approvedByName;
        String body = "Visitor approved for " + visitor.getVisitorName();

        for (User admin : security) {

            String fcmToken = admin.getFcmToken();

            if (fcmToken == null || fcmToken.isBlank()) {
                continue;
            }

            notificationDispatcher.sendAsync(
                    fcmToken,
                    title,
                    body);
        }
    }

    @Transactional
    public Visitor checkIn(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        if (visitor.getStatus() != VisitStatus.APPROVED) {
            throw new RuntimeException("Visitor not approved");
        }
        visitor.setStatus(VisitStatus.CHECKED_IN);
        visitor.setCheckInTime(LocalTime.now());
        visitor = visitorRepository.save(visitor);

        Optional<VisitorHistory> existingHistory = visitorHistoryRepository.findByVisitorId(visitorId).stream()
                .filter(h -> h.getCheckInTime() == null && h.getStatus() == VisitStatus.APPROVED)
                .findFirst();

        if (existingHistory.isPresent()) {
            VisitorHistory history = existingHistory.get();
            history.setStatus(visitor.getStatus());
            history.setCheckInTime(visitor.getCheckInTime());
            visitorHistoryRepository.save(history);
        } else {

            VisitorHistory historyEntry = VisitorHistory.builder()
                    .visitorId(visitor.getId())
                    .visitorName(visitor.getVisitorName())
                    .mobileNumber(visitor.getMobileNumber())
                    .visitType(visitor.getVisitType())
                    .idProof(visitor.getIdProof())
                    .imageUrl(visitor.getImageUrl())
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

        visitorHistoryRepository.findByVisitorIdAndCheckOutTimeIsNull(visitorId)
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
    public void deleteVisitor(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        visitorRepository.deleteById(visitorId);
    }

    public List<Visitor> getAllVisitorsForTenant(Long tenantId, User admin) {

        return visitorRepository.findAll().stream()
                .filter(v -> v.getTenant() != null && v.getTenant().getId().equals(tenantId))
                .toList();
    }
}
