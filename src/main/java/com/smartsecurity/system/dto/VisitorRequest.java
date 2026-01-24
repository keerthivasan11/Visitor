package com.smartsecurity.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisitorRequest {
    @NotBlank(message = "Visitor name is required")
    private String visitorName;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    private String visitType;
    private String idProof;
    private String imageUrl; // Base64 encoded image or URL
    private LocalDate visitDate;
  

    @NotNull(message = "Tenant ID is required")
    private Long tenantId; // Single tenant ID

    private Long assignedAdminId; // Backward compatibility for single ID
    private List<Long> assignedAdminIds; // Multiple assigned tenant admin IDs

    private Long createdByUserId;

    @JsonIgnore
    public List<Long> getEffectiveAdminIds() {
        List<Long> ids = new ArrayList<>();
        if (assignedAdminIds != null) {
            ids.addAll(assignedAdminIds);
        }
        if (assignedAdminId != null && !ids.contains(assignedAdminId)) {
            ids.add(assignedAdminId);
        }
        return ids;
    }
}
