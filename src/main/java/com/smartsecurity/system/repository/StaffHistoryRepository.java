package com.smartsecurity.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartsecurity.system.entity.StaffHistory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffHistoryRepository extends JpaRepository<StaffHistory, Integer> {

    Optional<StaffHistory> findByStaffIdAndCheckOutTimeIsNull(Integer staffId);

    @Query("""
                SELECT s FROM StaffHistory s
                WHERE s.checkInTime BETWEEN :start AND :end
            """)
    Page<StaffHistory> findByFilters(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("""
                SELECT s FROM StaffHistory s
                WHERE (:staffId IS NULL OR s.staffId = :staffId)
                  AND s.checkInTime >= COALESCE(:start, s.checkInTime)
                  AND s.checkInTime <= COALESCE(:end, s.checkInTime)
            """)
    Page<StaffHistory> findByStaffIdWithFilters(
            @Param("staffId") Long staffId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("""
               SELECT s FROM StaffHistory s
               WHERE s.checkInTime BETWEEN :start AND :end
            """)
    List<StaffHistory> findByFiltersWithoutPagination(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

}
