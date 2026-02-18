package com.smartsecurity.system.dto;

import java.util.Set;

import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantResponse {
    private Long id;
    private String companyName;
    private String companyCode;
    private Integer floorNumber;
    private String officeNumber;
    private String block;
    private UserStatus status;
    // private Set<User> admins;
    private Set<AdminResponse> admins;
}
