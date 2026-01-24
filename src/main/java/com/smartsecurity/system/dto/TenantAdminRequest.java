package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantAdminRequest {
    private String fullName;
    private String email;
    private String mobileNumber;
    private String idProof;
    private String password;
    private UserStatus status;
}
