package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VehicleType;
import com.smartsecurity.system.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleRequest {
    private VehicleType vehicleType;
    private String vehicleNumber;
    private String driverName;
    private String company;
    private String purpose;
    private Long tenantId;
    private Long createdByUserId;
    private UserType userType;
}
