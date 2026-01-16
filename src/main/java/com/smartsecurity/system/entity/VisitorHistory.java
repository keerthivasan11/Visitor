package com.smartsecurity.system.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.smartsecurity.system.enums.VisitStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "visitor_id", nullable = false)
    @JsonBackReference("visitor-history")
    private Visitor visitor;

    private String visitorName;
    private String mobileNumber;
    private String visitType;
    private String employeeToMeet;

    @Enumerated(EnumType.STRING)
    private VisitStatus status;

    private LocalDate visitDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
}
