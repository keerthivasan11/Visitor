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
public class TenantRequest {
    private String companyName;
    private String companyCode;
    private Integer floorNumber;
    private String officeNumber;
    private UserStatus status;
}
