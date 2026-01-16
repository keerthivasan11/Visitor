package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VisitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalRequest {
    private VisitStatus status;
    private String remarks;
}
