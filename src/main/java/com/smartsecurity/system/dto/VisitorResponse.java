package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VisitStatus;

import java.time.LocalDate;
import lombok.Data;
@Data
public class VisitorResponse {
    private Long id;
    private String visitorName;
    private String mobileNumber;
    private String visitType;
    private LocalDate visitDate;
    private VisitStatus status;
    private String comments;
    private String attachment;
}
