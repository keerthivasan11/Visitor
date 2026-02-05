package com.smartsecurity.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartsecurity.system.enums.VisitStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visitors")
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String visitorName;

    @Column(nullable = false)
    private String mobileNumber;

    private String visitType;
    private String idProof;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private LocalDate visitDate;

    @Enumerated(EnumType.STRING)
    private VisitStatus status;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    @JsonIgnoreProperties({ "admins", "vehicles" })
    private Tenant tenant;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "created_by")
    private Integer createdBy;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @JoinTable(name = "visitor_admins", joinColumns = @JoinColumn(name = "visitor_id"), inverseJoinColumns = @JoinColumn(name = "admin_id"))
    @JsonIgnoreProperties({ "password", "tenant" })
    private Set<User> assignedAdmins;

    private String rejectionRemarks;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

}
