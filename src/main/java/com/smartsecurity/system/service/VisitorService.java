package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.AdminResponse;
import com.smartsecurity.system.dto.ApprovalRequest;
import com.smartsecurity.system.dto.VisitorApprovalResponse;
import com.smartsecurity.system.dto.VisitorCheckInResponse;
import com.smartsecurity.system.dto.VisitorRequest;
import com.smartsecurity.system.dto.VisitorResponse;
import com.smartsecurity.system.dto.VisitorUpdateResponse;
import com.smartsecurity.system.entity.File;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.smartsecurity.system.entity.Visitor;
import java.io.IOException;

import com.smartsecurity.system.enums.VisitStatus;
import com.smartsecurity.system.repository.FileRepository;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.repository.VisitorHistoryRepository;
import com.smartsecurity.system.repository.VisitorRepository;
import com.smartsecurity.system.entity.VisitorHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final VisitorHistoryRepository visitorHistoryRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final FileRepository fileRepository;

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
    public VisitorResponse scheduleVisitor(VisitorRequest request, User tenantAdmin) {
        try {
            Visitor visitor = Visitor.builder()
                    .visitorName(request.getVisitorName())
                    .mobileNumber(request.getMobileNumber())
                    .visitType(request.getVisitType())
                    .visitDate(request.getVisitDate())
                    .status(VisitStatus.APPROVED)
                    .createdBy(tenantAdmin.getId())
                    .tenant(tenantAdmin.getTenant())
                    .comments(request.getComments())
                    .build();

            visitor = visitorRepository.save(visitor);

            if (request.getAttachment() != null) {

                String base64 = request.getAttachment();
                String fileType = "image/jpeg";

                if (base64.contains(",")) {
                    String[] parts = base64.split(",");
                    fileType = parts[0].split(":")[1].split(";")[0];
                    base64 = parts[1];
                }

                File file = File.builder()
                        .fileName("scheduled_visitor_attachment")
                        .fileType(fileType)
                        .fileData(base64)
                        .visitor(visitor)
                        .build();

                file = fileRepository.save(file);
            }

            return VisitorResponse.builder()
                    .id(visitor.getId())
                    .visitorName(visitor.getVisitorName())
                    .mobileNumber(visitor.getMobileNumber())
                    .visitType(visitor.getVisitType())
                    .visitDate(visitor.getVisitDate())
                    .status(visitor.getStatus())
                    .comments(visitor.getComments())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to schedule visitor: " + e.getMessage(), e);
        }
    }

    @Transactional
    public VisitorUpdateResponse updateScheduledVisitor(Long visitorId, VisitorRequest request, User tenantAdmin) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        boolean belongsToTenant = visitor.getTenant() != null &&
                visitor.getTenant().getId().equals(tenantAdmin.getTenant().getId());

        if (!belongsToTenant) {
            throw new RuntimeException("You can only update visitors for your tenant");
        }

        if (visitor.getStatus() == VisitStatus.CHECKED_OUT) {
            throw new RuntimeException("Cannot update visitor who has already checkedout");
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
        if (request.getComments() != null) {
            visitor.setComments(request.getComments());
        }
        if (request.getAttachment() != null) {

            Optional<File> existingFileOpt = fileRepository.findByVisitor(visitor);

            String base64 = request.getAttachment();
            String fileType = "image/jpeg";

            if (base64.contains(",")) {
                String[] parts = base64.split(",");
                fileType = parts[0].split(":")[1].split(";")[0];
                base64 = parts[1];
            }

            if (existingFileOpt.isPresent()) {

                File existingFile = existingFileOpt.get();
                existingFile.setFileData(base64);
                existingFile.setFileType(fileType);

                fileRepository.save(existingFile);

            } else {

                File newFile = File.builder()
                        .fileName("visitor_attachment")
                        .fileType(fileType)
                        .fileData(base64)
                        .visitor(visitor)
                        .build();

                fileRepository.save(newFile);
            }
        }
        String attachment = null;

        Optional<File> fileOpt = fileRepository.findByVisitorId(visitor.getId());
        if (fileOpt.isPresent()) {
            attachment = fileOpt.get().getFileData(); // base64
        }

        return new VisitorUpdateResponse(
                visitor.getId(),
                visitor.getVisitorName(),
                visitor.getMobileNumber(),
                visitor.getVisitType(),
                visitor.getIdProof(),
                visitor.getImageUrl(),
                visitor.getStatus(),
                visitor.getVisitDate(),
                visitor.getComments(), attachment);
    }

    @Transactional
    public VisitorResponse addWalkInVisitor(VisitorRequest request) {
        List<Long> adminIds = request.getEffectiveAdminIds();

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Long createdByUserId = request.getCreatedByUserId();

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
                .comments(request.getComments())
                .assignedAdmins(new HashSet<>())
                .build();

        visitor = visitorRepository.save(visitor);

        if (request.getAttachment() != null) {

            String base64 = request.getAttachment();
            String fileType = "image/jpeg";

            if (base64.contains(",")) {
                String[] parts = base64.split(",");
                fileType = parts[0].split(":")[1].split(";")[0];
                base64 = parts[1];
            }

            File file = File.builder()
                    .fileName("visitor_attachment")
                    .fileType(fileType)
                    .fileData(base64)
                    .createdAt(LocalDateTime.now())
                    .visitor(visitor)
                    .build();

            fileRepository.save(file);
        }

        if (!adminIds.isEmpty()) {

            Set<User> adminsToAssign = new HashSet<>();
            for (Long adminId : adminIds) {
                User admin = userRepository.findById(adminId)
                        .orElseThrow(() -> new RuntimeException("Assigned admin not found: " + adminId));

                adminsToAssign.add(admin);
            }

            visitor.setAssignedAdmins(adminsToAssign);
            visitor = visitorRepository.save(visitor);
        }

        sendVisitorCreatedNotifications(visitor);
        return mapToVisitorResponse(visitor);
    }

    private VisitorResponse mapToVisitorResponse(Visitor visitor) {

        Set<AdminResponse> adminResponses = new HashSet<>();

        for (User admin : visitor.getAssignedAdmins()) {
            adminResponses.add(
                    AdminResponse.builder()
                            .id(admin.getId())
                            .fullName(admin.getFullName())
                            .email(admin.getEmail())
                            .build());
        }

        return VisitorResponse.builder()
                .id(visitor.getId())
                .visitorName(visitor.getVisitorName())
                .mobileNumber(visitor.getMobileNumber())
                .visitType(visitor.getVisitType())
                .visitDate(visitor.getVisitDate())
                .status(visitor.getStatus())
                .comments(visitor.getComments())
                .admins(adminResponses)
                .tenantId(visitor.getTenant().getId())
                .tenantName(visitor.getTenant().getCompanyName())
                .build();
    }

    private void sendVisitorCreatedNotifications(Visitor visitor) {

        if (visitor.getAssignedAdmins() == null ||
                visitor.getAssignedAdmins().isEmpty()) {
            return;
        }

        String title = "New Walk-in Visitor";
        String body = "Visitor " + visitor.getVisitorName()
                + " is waiting for approval.";

        Set<String> uniqueTokens = visitor.getAssignedAdmins().stream()
                .map(User::getFcmToken)
                .filter(token -> token != null && !token.isBlank())
                .collect(Collectors.toSet());

        for (String token : uniqueTokens) {
            notificationDispatcher.sendAsync(token, title, body);
        }
    }

    // @Transactional
    // public VisitorApprovalResponse approveOrReject(Long visitorId,
    // ApprovalRequest request,
    // User admin) {

    // User adminUser = userRepository.findById(admin.getId())
    // .orElseThrow(() -> new RuntimeException("Admin not found"));

    // Visitor visitor = visitorRepository.findById(visitorId)
    // .orElseThrow(() -> new RuntimeException("Visitor not found"));

    // if (visitor.getStatus() != VisitStatus.PENDING) {
    // throw new RuntimeException("Already processed: " + visitor.getStatus());
    // }

    // if (!visitor.getTenant().getId().equals(adminUser.getTenant().getId())) {
    // throw new RuntimeException("Unauthorized tenant access");
    // }

    // if (visitor.getAssignedAdmins() != null &&
    // !visitor.getAssignedAdmins().isEmpty()) {
    // boolean isAssigned = visitor.getAssignedAdmins().stream()
    // .anyMatch(a -> a.getId().equals(adminUser.getId()));

    // if (!isAssigned) {
    // throw new RuntimeException("Not assigned to this visitor");
    // }
    // }

    // if (request.getStatus() != VisitStatus.APPROVED &&
    // request.getStatus() != VisitStatus.REJECTED) {
    // throw new RuntimeException("Invalid approval status");
    // }

    // visitor.setStatus(request.getStatus());
    // visitor.setApprovedBy(adminUser.getId());
    // visitor.setRejectionRemarks(request.getRemarks());

    // visitor = visitorRepository.save(visitor);

    // File file = visitor.getAttachments() != null
    // ? visitor.getAttachments().stream().findFirst().orElse(null)
    // : null;

    // VisitorHistory historyEntry = VisitorHistory.builder()
    // .visitorId(visitor.getId())
    // .visitorName(visitor.getVisitorName())
    // .mobileNumber(visitor.getMobileNumber())
    // .visitType(visitor.getVisitType())
    // .idProof(visitor.getIdProof())
    // .imageUrl(visitor.getImageUrl())
    // .status(visitor.getStatus())
    // .visitDate(visitor.getVisitDate())
    // .tenant(visitor.getTenant())
    // .createdBy(visitor.getCreatedBy())
    // .comments(visitor.getComments())
    // .approvedBy(adminUser.getId())
    // .rejectionRemarks(visitor.getRejectionRemarks())
    // .fileName(file != null ? file.getFileName() : null)
    // .fileUrl(file != null ? file.getFileData() : null)
    // .build();

    // visitorHistoryRepository.save(historyEntry);

    // // Send after DB save
    // sendSecurityCreatedNotifications(visitor);

    // return new VisitorApprovalResponse(
    // visitor.getId(),
    // visitor.getVisitorName(),
    // visitor.getMobileNumber(),
    // visitor.getStatus(),
    // visitor.getRejectionRemarks(),
    // visitor.getApprovedBy());
    // }

    @Transactional
    public VisitorApprovalResponse approveOrReject(Long visitorId,
            ApprovalRequest request,
            User admin) {

        // Fetch admin
        User adminUser = userRepository.findById(admin.getId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Fetch visitor
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        // If already processed → DO NOT send notification again
        if (visitor.getStatus() != VisitStatus.PENDING) {

            return new VisitorApprovalResponse(
                    visitor.getId(),
                    visitor.getVisitorName(),
                    visitor.getMobileNumber(),
                    visitor.getStatus(),
                    visitor.getRejectionRemarks(),
                    visitor.getApprovedBy());
        }

        // Tenant validation
        if (!visitor.getTenant().getId().equals(adminUser.getTenant().getId())) {
            throw new RuntimeException("Unauthorized tenant access");
        }

        // Assigned admin validation (if applicable)
        if (visitor.getAssignedAdmins() != null && !visitor.getAssignedAdmins().isEmpty()) {

            boolean isAssigned = visitor.getAssignedAdmins().stream()
                    .anyMatch(a -> a.getId().equals(adminUser.getId()));

            if (!isAssigned) {
                throw new RuntimeException("Not assigned to this visitor");
            }
        }

        // Validate approval status
        if (request.getStatus() != VisitStatus.APPROVED &&
                request.getStatus() != VisitStatus.REJECTED) {
            throw new RuntimeException("Invalid approval status");
        }

        // ✅ Update status (only once)
        visitor.setStatus(request.getStatus());
        visitor.setApprovedBy(adminUser.getId());
        visitor.setRejectionRemarks(request.getRemarks());

        visitor = visitorRepository.save(visitor);

        // Save history
        File file = visitor.getAttachments() != null
                ? visitor.getAttachments().stream().findFirst().orElse(null)
                : null;

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
                .comments(visitor.getComments())
                .approvedBy(adminUser.getId())
                .rejectionRemarks(visitor.getRejectionRemarks())
                .fileName(file != null ? file.getFileName() : null)
                .fileUrl(file != null ? file.getFileData() : null)
                .build();

        visitorHistoryRepository.save(historyEntry);

        // ✅ Send notification ONLY ONCE
        sendSecurityCreatedNotifications(visitor);

        return new VisitorApprovalResponse(
                visitor.getId(),
                visitor.getVisitorName(),
                visitor.getMobileNumber(),
                visitor.getStatus(),
                visitor.getRejectionRemarks(),
                visitor.getApprovedBy());
    }

    private void sendSecurityCreatedNotifications(Visitor visitor) {

        if (visitor == null || visitor.getCreatedBy() == null) {
            return;
        }

        User securityUser = userRepository.findById(visitor.getCreatedBy())
                .orElse(null);

        if (securityUser == null) {
            return;
        }

        String fcmToken = securityUser.getFcmToken();

        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        String approvedByName = "Admin";

        if (visitor.getApprovedBy() != null) {
            approvedByName = userRepository
                    .findById(visitor.getApprovedBy())
                    .map(User::getFullName)
                    .orElse("Admin");
        }

        String title;
        String body;

        if (visitor.getStatus() == VisitStatus.APPROVED) {

            title = "Visitor Approved By " + approvedByName;
            body = "Visitor approved for " + visitor.getVisitorName();

        } else if (visitor.getStatus() == VisitStatus.REJECTED) {

            title = "Visitor Rejected By " + approvedByName;
            body = "Visitor rejected for " + visitor.getVisitorName();

        } else {
            return;
        }
        notificationDispatcher.sendAsync(fcmToken, title, body);
    }

    @Transactional
    public VisitorCheckInResponse checkIn(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        if (visitor.getStatus() != VisitStatus.APPROVED) {
            throw new RuntimeException("Visitor not approved");
        }
        LocalDateTime istNow = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        visitor.setStatus(VisitStatus.CHECKED_IN);
        visitor.setCheckInTime(istNow);
        Optional<VisitorHistory> existingHistory = visitorHistoryRepository.findByVisitorId(visitorId).stream()
                .filter(h -> h.getCheckInTime() == null && h.getStatus() == VisitStatus.APPROVED)
                .findFirst();
        if (existingHistory.isPresent()) {
            VisitorHistory history = existingHistory.get();
            history.setStatus(visitor.getStatus());
            history.setCheckInTime(visitor.getCheckInTime());
        } else {
            File file = null;
            if (visitor.getAttachments() != null && !visitor.getAttachments().isEmpty()) {
                file = visitor.getAttachments().iterator().next();
            }
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
                    .comments(visitor.getComments())
                    .createdBy(visitor.getCreatedBy())
                    .fileName(file != null ? file.getFileName() : null)
                    .fileUrl(file != null ? file.getFileData() : null)
                    .build();
            visitorHistoryRepository.save(historyEntry);
        }
        return new VisitorCheckInResponse(
                visitor.getId(),
                visitor.getVisitorName(),
                visitor.getStatus(),
                visitor.getCheckInTime());

    }

    @Transactional
    public Visitor checkOut(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        LocalDateTime istNow = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        visitor.setStatus(VisitStatus.CHECKED_OUT);
        visitor.setCheckOutTime(istNow);
        visitorHistoryRepository.findByVisitorIdAndCheckOutTimeIsNull(visitorId)
                .ifPresent(history -> {
                    history.setStatus(visitor.getStatus());
                    history.setCheckOutTime(visitor.getCheckOutTime());
                    visitorHistoryRepository.save(history);
                });
        return visitorRepository.save(visitor);
    }

    public Page<VisitorHistory> getVisitorHistory(Long visitorId, int page, int size, LocalDateTime start,
            LocalDateTime end) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkInTime").descending());
        return visitorHistoryRepository.findByVisitorIdWithFilters(visitorId, start, end, pageable);
    }

    @Transactional
    public void deleteVisitor(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        fileRepository.deleteByVisitor(visitor);
        visitorRepository.delete(visitor);
    }

    public List<VisitorResponse> getAllVisitorsForTenant(Long tenantId, User admin) {
        List<Visitor> visitors = visitorRepository.findByTenant_Id(tenantId);
        return visitors.stream().map(visitor -> {
            VisitorResponse response = new VisitorResponse();
            response.setId(visitor.getId());
            response.setVisitorName(visitor.getVisitorName());
            response.setMobileNumber(visitor.getMobileNumber());
            response.setVisitType(visitor.getVisitType());
            response.setImageUrl(visitor.getImageUrl());
            response.setVisitDate(visitor.getVisitDate());
            response.setStatus(visitor.getStatus());
            response.setComments(visitor.getComments());
            fileRepository.findByVisitor_Id(visitor.getId())
                    .ifPresent(file -> {
                        response.setAttachment(file.getFileData());
                    });
            return response;
        }).toList();
    }

    public File getVisitorFile(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        return fileRepository.findByVisitor(visitor)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

}
