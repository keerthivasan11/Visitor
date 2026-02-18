package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VisitStatus;

import lombok.Data;
@Data
public class VisitorApprovalResponse {
    private Long id;
    private String visitorName;
    private String mobileNumber;
    private VisitStatus status;
    private String rejectionRemarks;
    private Long approvedBy;

    public VisitorApprovalResponse(Long id, String visitorName,
            String mobileNumber,
            VisitStatus status,
            String rejectionRemarks,
            Long approvedBy) {
        this.id = id;
        this.visitorName = visitorName;
        this.mobileNumber = mobileNumber;
        this.status = status;
        this.rejectionRemarks = rejectionRemarks;
        this.approvedBy = approvedBy;
    }
}
