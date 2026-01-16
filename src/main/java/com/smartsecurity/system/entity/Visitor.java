package com.smartsecurity.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartsecurity.system.enums.VisitStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    private String visitType; // Interview, Guest, Vendor
    private String idProof;

    @Column(columnDefinition = "TEXT")
    private String imageUrl; // Visitor photo captured from camera

    private LocalDate visitDate;
    private LocalTime expectedTime;
    private String employeeToMeet;

    @Enumerated(EnumType.STRING)
    private VisitStatus status;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    @JsonIgnoreProperties({ "admins", "vehicles" })
    private Tenant tenant; // The company they are visiting

    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    @JsonIgnoreProperties({ "password", "tenant" })
    private User approvedBy;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    @JsonIgnoreProperties({ "password", "tenant" })
    private User createdBy;

    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @JoinTable(name = "visitor_admins", joinColumns = @JoinColumn(name = "visitor_id"), inverseJoinColumns = @JoinColumn(name = "admin_id"))
    @JsonIgnoreProperties({ "password", "tenant" })
    private Set<User> assignedAdmins;

    private String rejectionRemarks;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    @OneToMany(mappedBy = "visitor", cascade = CascadeType.ALL)
    @JsonManagedReference("visitor-history")
    private List<VisitorHistory> history;
}
