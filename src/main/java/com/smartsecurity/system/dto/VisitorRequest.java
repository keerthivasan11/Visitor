package com.smartsecurity.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private String imageUrl; 
    private LocalDate visitDate;
    private String comments;
    private String attachment;

    @NotNull(message = "Tenant ID is required")
    private Long tenantId; 

    private Integer assignedAdminId; 
    private List<Integer> assignedAdminIds; 

    private Integer createdByUserId;

    @JsonIgnore
    public List<Integer> getEffectiveAdminIds() {
        List<Integer> ids = new ArrayList<>();
        if (assignedAdminIds != null) {
            ids.addAll(assignedAdminIds);
        }
        if (assignedAdminId != null && !ids.contains(assignedAdminId)) {
            ids.add(assignedAdminId);
        }
        return ids;
    }
}
