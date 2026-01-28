package com.smartsecurity.system.entity;

import java.time.LocalDateTime;

import com.smartsecurity.system.enums.VisitStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "staff_history")
public class StaffHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "address")
    private String address;

    @Column(name = "name")
    private String name;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "id_proof")
    private String idProof;

    @Enumerated(EnumType.STRING)
    private VisitStatus status;

    @Column(name = "created_by")
    private Integer createdBy;

    // @Column(name = "updated_by")
    // private Integer updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // @Column(nullable = false, updatable = false)
    // private LocalDateTime updatedAt;

    private long staffId;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

}
