package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VisitStatus;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VisitorUpdateResponse {
    private Long id;
    private String visitorName;
    private String mobileNumber;
    private String visitType;
    private String idProof;
    private String imageUrl;
    private VisitStatus status;
    private LocalDate visitDate;
    private String comments;
    private String attachment;

    public VisitorUpdateResponse(Long id,
            String visitorName,
            String mobileNumber,
            String visitType,
            String idProof,
            String imageUrl,
            VisitStatus status,
            LocalDate visitDate,
            String comments,
            String attachment) {
        this.id = id;
        this.visitorName = visitorName;
        this.mobileNumber = mobileNumber;
        this.visitType = visitType;
        this.idProof = idProof;
        this.imageUrl = imageUrl;
        this.status = status;
        this.visitDate = visitDate;
        this.comments = comments;
        this.attachment = attachment;
    }
}
