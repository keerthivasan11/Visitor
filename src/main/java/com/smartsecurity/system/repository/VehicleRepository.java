package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Vehicle;

import com.smartsecurity.system.enums.VehicleStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVehicleNumberAndCheckOutTimeIsNull(String vehicleNumber);

    List<Vehicle> findByTenant_Id(Long tenantId);

    long countByTenant_IdAndCheckOutTimeIsNull(Long tenantId);

    long countByStatus(VehicleStatus status);

    @Modifying
    @Query("UPDATE Vehicle v SET v.status = :status WHERE v.tenant.id = :tenantId")
    void updateStatusByTenantId(@Param("tenantId") Long tenantId,
            @Param("status") VehicleStatus status);

    @Query("SELECT v FROM Vehicle v WHERE v.status IN :statuses")
    List<Vehicle> findByStatusIn(@Param("statuses") List<VehicleStatus> statuses);

}
