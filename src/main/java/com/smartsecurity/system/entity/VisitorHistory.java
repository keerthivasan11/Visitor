package com.smartsecurity.system.entity;

import com.smartsecurity.system.enums.VisitStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visitor_history")
public class VisitorHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "visitor_id")
    private long visitorId;
    private String visitorName;
    private String mobileNumber;
    private String visitType;

    @Enumerated(EnumType.STRING)
    private VisitStatus status;

    private LocalDate visitDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    private String rejectionRemarks;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private String idProof;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "approved_by")
    private Integer approvedBy;
}
