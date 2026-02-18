package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.UserStatus;

import lombok.Data;
@Data
public class TenantAdminResponse {

    private Long id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private UserStatus status;

    public TenantAdminResponse(Long id, String fullName, String email,
            String mobileNumber, UserStatus status) {
                
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.status = status;
    }
}
