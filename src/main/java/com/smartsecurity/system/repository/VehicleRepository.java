package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Vehicle;
import com.smartsecurity.system.enums.VehicleStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVehicleNumberAndCheckOutTimeIsNull(String vehicleNumber);

    List<Vehicle> findByTenant_Id(Long tenantId);

    long countByTenant_IdAndCheckOutTimeIsNull(Long tenantId);

    long countByStatus(VehicleStatus status);

}
