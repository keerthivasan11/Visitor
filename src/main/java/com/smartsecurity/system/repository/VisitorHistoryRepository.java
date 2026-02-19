package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.VisitorHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Modifying;

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

    @Query("""
                SELECT v.visitDate, COUNT(v)
                FROM VisitorHistory v
                WHERE v.visitDate >= :start
                GROUP BY v.visitDate
                ORDER BY v.visitDate
            """)
    List<Object[]> countDailyVisitors(@Param("start") LocalDate start);

    @Query("""
                SELECT FUNCTION('date_trunc', 'week', v.visitDate), COUNT(v)
                FROM VisitorHistory v
                WHERE v.visitDate >= :start
                GROUP BY FUNCTION('date_trunc', 'week', v.visitDate)
                ORDER BY FUNCTION('date_trunc', 'week', v.visitDate)
            """)
    List<Object[]> countWeeklyVisitors(@Param("start") LocalDate start);

    @Query("""
               SELECT
                   YEAR(v.visitDate),
                   MONTH(v.visitDate),
                   COUNT(v)
               FROM VisitorHistory v
               WHERE v.visitDate >= :start
               GROUP BY YEAR(v.visitDate), MONTH(v.visitDate)
               ORDER BY YEAR(v.visitDate), MONTH(v.visitDate)
            """)
    List<Object[]> countMonthlyVisitors(@Param("start") LocalDate start);

    @Query("""
               SELECT v FROM VisitorHistory v
               WHERE (:tenantId IS NULL OR v.tenant.id = :tenantId)
               AND v.visitDate BETWEEN :start AND :end
            """)
    List<VisitorHistory> findByFiltersWithoutPagination(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    void deleteByTenant_Id(Long tenantId);

    @Modifying
    @Query(value = """
                UPDATE visitor_history vh
                SET status = v.status,
                    check_out_time =
                        CASE
                            WHEN v.status = 'CHECKED_IN' THEN :now
                            ELSE vh.check_out_time
                        END
                FROM visitors v
                WHERE vh.visitor_id = v.id
                  AND v.tenant_id = :tenantId
            """, nativeQuery = true)
    void syncStatusWithVisitor(@Param("tenantId") Long tenantId,
            @Param("now") LocalDateTime now);

}
