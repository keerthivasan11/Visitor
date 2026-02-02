package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.entity.VisitorHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitorHistoryRepository extends JpaRepository<VisitorHistory, Long> {

  List<VisitorHistory> findByVisitorId(Long visitorId);

  Optional<VisitorHistory> findByVisitorIdAndCheckOutTimeIsNull(Long visitorId);

  @Query("""
          SELECT v FROM VisitorHistory v
          WHERE (:tenantId IS NULL OR v.tenant.id = :tenantId)
            AND v.visitDate BETWEEN :startDate AND :endDate
      """)
  Page<VisitorHistory> findByFilters(
      @Param("tenantId") Long tenantId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      Pageable pageable);

  @Query("""
          SELECT vh FROM VisitorHistory vh
          WHERE (:visitorId IS NULL OR vh.visitorId = :visitorId)
            AND vh.checkInTime >= COALESCE(:start, vh.checkInTime)
            AND vh.checkInTime <= COALESCE(:end, vh.checkInTime)
      """)
  Page<VisitorHistory> findByVisitorIdWithFilters(
      @Param("visitorId") Long visitorId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      Pageable pageable);
}
