package com.smartsecurity.system.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminResponse {
    private Long id;
    private String fullName;
    private String email;

    private String mobileNumber;

    private String idProof;
}
