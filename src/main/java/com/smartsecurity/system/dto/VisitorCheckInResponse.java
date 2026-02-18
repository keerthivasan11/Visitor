package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VisitStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class VisitorCheckInResponse {
    private Long id;
    private String visitorName;
    private VisitStatus status;
    private LocalDateTime checkInTime;

    public VisitorCheckInResponse(Long id, String visitorName,
            VisitStatus status,
            LocalDateTime checkInTime) {
        this.id = id;
        this.visitorName = visitorName;
        this.status = status;
        this.checkInTime = checkInTime;
    }
}
