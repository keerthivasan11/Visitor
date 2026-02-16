package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.VehicleRequest;
import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.entity.Vehicle;
import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.enums.VehicleStatus;
import com.smartsecurity.system.repository.TenantRepository;

import com.smartsecurity.system.repository.VehicleHistoryRepository;
import com.smartsecurity.system.repository.VehicleRepository;
import com.smartsecurity.system.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final TenantRepository tenantRepository;
    private final VehicleHistoryRepository vehicleHistoryRepository;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getCheckedInVehicles() {
        return vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.PENDING ||
                        v.getStatus() == VehicleStatus.CHECKED_IN)
                .toList();
    }

    public List<Vehicle> getCheckedOutVehicles() {
        return vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.CHECKED_OUT)
                .toList();
    }

    @Transactional
    public Vehicle checkInVehicle(VehicleRequest request) {
        User currentUser = JwtAuthenticationFilter.getCurrentUser();
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

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(request.getVehicleNumber())
                .vehicleType(request.getVehicleType())
                .driverName(request.getDriverName())
                .company(tenant != null ? tenant.getCompanyName() : request.getCompany())
                .tenant(tenant)
                .purpose(request.getPurpose())
                .status(VehicleStatus.PENDING)
                .userType(request.getUserType())
                .createdAt(LocalDateTime.now())
                .createdBy(currentUser.getId())
                .build();
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle performCheckIn(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        LocalDateTime istNow = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        vehicle.setStatus(VehicleStatus.CHECKED_IN);
        vehicle.setCheckInTime(istNow);
        vehicle = vehicleRepository.save(vehicle);

        VehicleHistory historyRecord = VehicleHistory.builder()
                .vehicleId(vehicle.getId())
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

    @Transactional
    public Vehicle updateVehicle(Long vehicleId, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        updateVehicleFields(vehicle, request);

        if (request.getCompany() != null && request.getTenantId() == null) {
            tenantRepository.findByCompanyName(request.getCompany())
                    .ifPresent(t -> vehicle.setTenant(t));
        }

        if (request.getTenantId() != null) {
            tenantRepository.findById(request.getTenantId())
                    .ifPresent(v -> vehicle.setTenant(v));
        }

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateTenantVehicle(Long vehicleId, VehicleRequest request, Long tenantId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (vehicle.getTenant() == null || !vehicle.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized: This vehicle does not belong to your tenant");
        }

        updateVehicleFields(vehicle, request);

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    private void updateVehicleFields(Vehicle vehicle, VehicleRequest request) {
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
        }
        if (request.getPurpose() != null) {
            vehicle.setPurpose(request.getPurpose());
        }
        if (request.getUserType() != null) {
            vehicle.setUserType(request.getUserType());
        }
    }

    @Transactional
    public Vehicle checkOutVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        LocalDateTime istNow = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        if (vehicle.getStatus() != VehicleStatus.CHECKED_IN) {
            throw new RuntimeException("Vehicle is not checked in");
        }

        vehicle.setStatus(VehicleStatus.CHECKED_OUT);
        vehicle.setCheckOutTime(istNow);

        vehicleHistoryRepository.findByVehicleIdAndCheckOutTimeIsNull(vehicleId)
                .ifPresent(history -> {
                    history.setStatus(vehicle.getStatus());
                    history.setCheckOutTime(vehicle.getCheckOutTime());
                    vehicleHistoryRepository.save(history);
                });

        return vehicleRepository.save(vehicle);
    }

    public Page<VehicleHistory> getVehicleHistory(Long vehicleId, int page, int size, LocalDateTime start,
            LocalDateTime end) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkInTime").descending());
        return vehicleHistoryRepository.findByVehicleIdWithFilters(vehicleId, start, end, pageable);
    }

    public Optional<Vehicle> findByNumber(String number) {
        return vehicleRepository.findByVehicleNumberAndCheckOutTimeIsNull(number);
    }

    public List<Vehicle> getVehiclesByTenant(Long tenantId) {
        return vehicleRepository.findByTenant_Id(tenantId);
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicleRepository.deleteById(vehicleId);
    }

    @Transactional
    public void deleteTenantVehicle(Long vehicleId, Long tenantId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (vehicle.getTenant() == null || !vehicle.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized: This vehicle does not belong to your tenant");
        }

        vehicleRepository.delete(vehicle);
    }
}
