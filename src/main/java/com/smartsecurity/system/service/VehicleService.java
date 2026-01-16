package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.VehicleRequest;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.entity.Vehicle;
import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.enums.VehicleStatus;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.repository.VehicleHistoryRepository;
import com.smartsecurity.system.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final VehicleHistoryRepository vehicleHistoryRepository;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getCheckedInVehicles() {
        // Get vehicles that are PENDING or CHECKED_IN (for Tab 1)
        return vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.PENDING ||
                        v.getStatus() == VehicleStatus.CHECKED_IN)
                .toList();
    }

    public List<Vehicle> getCheckedOutVehicles() {
        // Get vehicles that are CHECKED_OUT (for Tab 2)
        return vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.CHECKED_OUT)
                .toList();
    }

    public Vehicle checkInVehicle(VehicleRequest request) {
        // // 1. Validation: Prevent duplicate active entries
        // vehicleRepository.findByVehicleNumberAndCheckOutTimeIsNull(request.getVehicleNumber())
        // .ifPresent(v -> {
        // throw new RuntimeException("Vehicle " + request.getVehicleNumber() + " is
        // already inside.");
        // });

        // // 2. Resolve Tenant (by ID or Company Name)
        // Tenant tenant = null;
        // if (request.getTenantId() != null) {
        // tenant = tenantRepository.findById(request.getTenantId()).orElse(null);
        // } else if (request.getCompany() != null) {
        // tenant =
        // tenantRepository.findByCompanyName(request.getCompany()).orElse(null);
        // }

        // // 3. Resolve User
        // User createdByUser = userRepository.findById(request.getCreatedByUserId())
        // .orElseThrow(() -> new RuntimeException("User not found with ID: " +
        // request.getCreatedByUserId()));

        // // 4. Role-Based Logic
        // String qrToken = null;
        // VehicleStatus initialStatus = VehicleStatus.PENDING;
        // LocalDateTime checkInTime = null;

        // if ("TENANT".equalsIgnoreCase(String.valueOf(request.getUserType()))) {
        // qrToken = UUID.randomUUID().toString(); // Generate QR for Pre-Registration
        // } else if
        // ("SECURITY".equalsIgnoreCase(String.valueOf(request.getUserType()))) {
        // initialStatus = VehicleStatus.PENDING; // Security check-ins are immediate
        // checkInTime = LocalDateTime.now();
        // }

        // // 5. Build and Save
        // Vehicle vehicle = Vehicle.builder()
        // .vehicleNumber(request.getVehicleNumber())
        // .vehicleType(request.getVehicleType())
        // .driverName(request.getDriverName())
        // .company(request.getCompany())
        // .tenant(tenant)
        // .purpose(request.getPurpose())
        // .status(initialStatus)
        // .userType(request.getUserType())
        // .qrToken(qrToken)
        // .checkInTime(checkInTime) // Populated only if Security adds it
        // .createdAt(LocalDateTime.now())
        // .createdBy(createdByUser)
        // .build();

        // return vehicleRepository.save(vehicle);
        Optional<Vehicle> existingActive = vehicleRepository
                .findByVehicleNumberAndCheckOutTimeIsNull(request.getVehicleNumber());
        if (existingActive.isPresent()) {
            throw new RuntimeException("Vehicle already inside");
        }

        Long tenantId = request.getTenantId();
        if (tenantId == null && request.getCompany() != null) {
            tenantId = tenantRepository.findByCompanyName(request.getCompany())
                    .map(t -> t.getId())
                    .orElse(null);
        }

        Tenant tenant = null;
        if (tenantId != null) {
            tenant = tenantRepository.findById(tenantId).orElse(null);
        }

        User createdByUser = null;
        if (request.getCreatedByUserId() != null) {
            createdByUser = userRepository.findById(request.getCreatedByUserId()).orElse(null);
        }

        // Create vehicle with PENDING status (awaiting check-in button click)
        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(request.getVehicleNumber())
                .vehicleType(request.getVehicleType())
                .driverName(request.getDriverName())
                .company(request.getCompany())
                .tenant(tenant)
                .purpose(request.getPurpose())
                .status(VehicleStatus.PENDING)
                .userType(request.getUserType())
                .createdAt(LocalDateTime.now())
                .createdBy(createdByUser)
                .build();
        return vehicleRepository.save(vehicle);
    }

    public Vehicle performCheckIn(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        vehicle.setStatus(VehicleStatus.CHECKED_IN);
        vehicle.setCheckInTime(LocalDateTime.now());
        vehicle = vehicleRepository.save(vehicle);

        // Create history record at check-in
        VehicleHistory historyRecord = VehicleHistory.builder()
                .vehicle(vehicle)
                .vehicleType(vehicle.getVehicleType())
                .vehicleNumber(vehicle.getVehicleNumber())
                .driverName(vehicle.getDriverName())
                .company(vehicle.getCompany())
                .tenant(vehicle.getTenant())
                .purpose(vehicle.getPurpose())
                .status(vehicle.getStatus())
                .userType(vehicle.getUserType())
                .checkInTime(vehicle.getCheckInTime())
                .createdAt(LocalDateTime.now())
                .createdBy(vehicle.getCreatedBy())
                .build();

        vehicleHistoryRepository.save(historyRecord);

        return vehicle;
    }

    public Vehicle updateVehicle(Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Update fields if provided
        if (request.getVehicleNumber() != null) {
            vehicle.setVehicleNumber(request.getVehicleNumber());
        }
        if (request.getVehicleType() != null) {
            vehicle.setVehicleType(request.getVehicleType());
        }
        if (request.getDriverName() != null) {
            vehicle.setDriverName(request.getDriverName());
        }
        if (request.getCompany() != null) {
            vehicle.setCompany(request.getCompany());
            // Also try to update tenant if it was missing but company provided
            if (request.getTenantId() == null) {
                tenantRepository.findByCompanyName(request.getCompany())
                        .ifPresent(t -> vehicle.setTenant(t));
            }
        }
        if (request.getTenantId() != null) {
            tenantRepository.findById(request.getTenantId())
                    .ifPresent(v -> vehicle.setTenant(v));
        }
        if (request.getPurpose() != null) {
            vehicle.setPurpose(request.getPurpose());
        }
        if (request.getUserType() != null) {
            vehicle.setUserType(request.getUserType());
        }

        return vehicleRepository.save(vehicle);
    }

    public Vehicle checkOutVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (vehicle.getStatus() != VehicleStatus.CHECKED_IN) {
            throw new RuntimeException("Vehicle is not checked in");
        }

        vehicle.setStatus(VehicleStatus.CHECKED_OUT);
        vehicle.setCheckOutTime(LocalDateTime.now());

        // Update existing history record
        vehicleHistoryRepository.findByVehicleAndCheckOutTimeIsNull(vehicle)
                .ifPresent(history -> {
                    history.setStatus(vehicle.getStatus());
                    history.setCheckOutTime(vehicle.getCheckOutTime());
                    vehicleHistoryRepository.save(history);
                });

        return vehicleRepository.save(vehicle);
    }

    public List<VehicleHistory> getVehicleHistory(Long vehicleId) {
        return vehicleHistoryRepository.findByVehicleId(vehicleId);
    }

    public Optional<Vehicle> findByNumber(String number) {
        return vehicleRepository.findByVehicleNumberAndCheckOutTimeIsNull(number);
    }

    public List<Vehicle> getVehiclesByTenant(Long tenantId) {
        return vehicleRepository.findByTenant_Id(tenantId);
    }

    public void deleteVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicleRepository.delete(vehicle);
    }
}
