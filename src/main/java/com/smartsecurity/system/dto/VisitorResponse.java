package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VisitStatus;

import java.time.LocalDate;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisitorResponse {
    private Long id;
    private String visitorName;
    private String mobileNumber;
    private String visitType;
    private LocalDate visitDate;
    private VisitStatus status;
    private String comments;
    private String attachment;
    private Long tenantId;
     private String tenantName; 
    private Set<AdminResponse> admins;
}
