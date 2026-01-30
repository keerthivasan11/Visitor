package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.VehicleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleHistoryRepository extends JpaRepository<VehicleHistory, Long> {

  List<VehicleHistory> findByVehicleId(Long vehicleId);

  Optional<VehicleHistory> findByVehicleIdAndCheckOutTimeIsNull(Long vehicleId);

  @Query("""
          SELECT v FROM VehicleHistory v
          WHERE (:tenantId IS NULL OR v.tenant.id = :tenantId)
            AND v.checkInTime BETWEEN :start AND :end
      """)
  Page<VehicleHistory> findByFilters(
      @Param("tenantId") Long tenantId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      Pageable pageable);

  // @Query("""
  // SELECT v FROM VehicleHistory v
  // WHERE v.vehicleId = :vehicleId
  // AND (:start IS NULL OR v.checkInTime >= :start)
  // AND (:end IS NULL OR v.checkInTime <= :end)
  // """)
  // Page<VehicleHistory> findByVehicleIdWithFilters(
  // @Param("vehicleId") Long vehicleId,
  // @Param("start") LocalDateTime start,
  // @Param("end") LocalDateTime end,
  // Pageable pageable);

  @Query("""
          SELECT v FROM VehicleHistory v
          WHERE v.vehicleId = :vehicleId
            AND v.checkInTime >= :start
            AND v.checkInTime <= :end
      """)
  Page<VehicleHistory> findByVehicleIdWithFilters(
      @Param("vehicleId") Long vehicleId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      Pageable pageable);

}
