package com.smartsecurity.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartsecurity.system.entity.StaffHistory;
import com.smartsecurity.system.entity.VehicleHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffHistoryRepository extends JpaRepository<StaffHistory, Integer> {

  Optional<StaffHistory> findByStaffIdAndCheckOutTimeIsNull(Integer staffId);

  @Query("""
          SELECT vh
          FROM StaffHistory vh
          WHERE vh.checkInTime BETWEEN :start AND :end
      """)
  List<StaffHistory> findByFilters(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

}
