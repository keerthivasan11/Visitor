package com.smartsecurity.system.dto;

import com.smartsecurity.system.enums.VehicleStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class VehicleResponse {
 private Long id;
    private String vehicleNumber;
    private String driverName;
    private VehicleStatus status;
    private LocalDateTime checkInTime;

    public VehicleResponse(Long id, String vehicleNumber,
                           String driverName,
                           VehicleStatus status,
                           LocalDateTime checkInTime) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.driverName = driverName;
        this.status = status;
        this.checkInTime = checkInTime;
    }
}
