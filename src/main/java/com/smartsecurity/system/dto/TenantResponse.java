package com.smartsecurity.system.dto;

import java.util.List;

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
    private UserStatus status;
    private List<User> admins;
}
