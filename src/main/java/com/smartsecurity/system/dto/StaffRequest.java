package com.smartsecurity.system.dto;


import java.time.LocalDateTime;


import com.smartsecurity.system.enums.VisitStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffRequest {

    private Long id;

    private String employeeCode;

    private String address;

    private String name;

    private String mobileNumber;

    private String idProof;

    private VisitStatus status;

    private Integer createdBy;

    // private Integer updatedBy;

    private LocalDateTime createdAt;

    // private LocalDateTime updatedAt;

}
