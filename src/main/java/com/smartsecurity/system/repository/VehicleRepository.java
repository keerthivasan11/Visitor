package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Vehicle;
import com.smartsecurity.system.enums.VehicleStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVehicleNumberAndCheckOutTimeIsNull(String vehicleNumber);

    List<Vehicle> findByTenant_Id(Long tenantId);

    long countByTenant_IdAndCheckOutTimeIsNull(Long tenantId);

    long countByStatus(VehicleStatus status);

    @Query("""
                SELECT DATE(v.checkInTime), COUNT(v)
                FROM Vehicle v
                WHERE v.checkInTime >= :start
                GROUP BY DATE(v.checkInTime)
                ORDER BY DATE(v.checkInTime)
            """)
    List<Object[]> countDailyVehicles(@Param("start") LocalDateTime start);

    @Query("""
                SELECT FUNCTION('date_trunc', 'week', v.checkInTime), COUNT(v)
                FROM Vehicle v
                WHERE v.checkInTime >= :start
                GROUP BY FUNCTION('date_trunc', 'week', v.checkInTime)
                ORDER BY FUNCTION('date_trunc', 'week', v.checkInTime)
            """)
    List<Object[]> countWeeklyVehicles(@Param("start") LocalDateTime start);

    @Query("""
                SELECT TO_CHAR(v.checkInTime, 'YYYY-MM'), COUNT(v)
                FROM VehicleHistory v
                WHERE v.checkInTime >= :start
                GROUP BY TO_CHAR(v.checkInTime, 'YYYY-MM')
                ORDER BY TO_CHAR(v.checkInTime, 'YYYY-MM')
            """)
    List<Object[]> countMonthlyVehicles(@Param("start") LocalDateTime start);

}
