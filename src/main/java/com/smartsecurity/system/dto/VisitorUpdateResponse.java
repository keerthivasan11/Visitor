package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VisitStatus;

import lombok.Data;

import java.time.LocalDate;
@Data
public class VisitorUpdateResponse {
    private Long id;
    private String visitorName;
    private String mobileNumber;
    private VisitStatus status;
    private LocalDate visitDate;
    private String comments;

    public VisitorUpdateResponse(Long id,
            String visitorName,
            String mobileNumber,
            VisitStatus status,
            LocalDate visitDate,
            String comments) {
        this.id = id;
        this.visitorName = visitorName;
        this.mobileNumber = mobileNumber;
        this.status = status;
        this.visitDate = visitDate;
        this.comments = comments;
    }
}
