package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Vehicle;
import com.smartsecurity.system.entity.VehicleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleHistoryRepository extends JpaRepository<VehicleHistory, Long> {
     

        List<VehicleHistory> findByVehicleId(Long vehicleId);

        Optional<VehicleHistory> findByVehicleIdAndCheckOutTimeIsNull(Long vehicleId);

        @org.springframework.data.jpa.repository.Query("SELECT vh FROM VehicleHistory vh WHERE " +
                        "(:tenantId IS NULL OR vh.tenant.id = :tenantId) AND " +
                        "vh.checkInTime BETWEEN :start AND :end")
        List<VehicleHistory> findByFilters(
                        @org.springframework.data.repository.query.Param("tenantId") Long tenantId,
                        @org.springframework.data.repository.query.Param("start") LocalDateTime start,
                        @org.springframework.data.repository.query.Param("end") LocalDateTime end);
}
