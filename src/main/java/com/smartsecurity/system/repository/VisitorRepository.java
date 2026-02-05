package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Visitor;
import com.smartsecurity.system.enums.VisitStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import java.util.List;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {
        List<Visitor> findByVisitDate(LocalDate date);

        long countByVisitDateAndStatusIn(LocalDate date, List<VisitStatus> statuses);

        @Query("""
                            SELECT DISTINCT v
                            FROM Visitor v
                            JOIN v.assignedAdmins a
                            WHERE v.status = :status
                              AND v.tenant.id = :tenantId
                              AND a.id = :adminId
                        """)
        List<Visitor> findPendingForAdmin(
                        @Param("status") VisitStatus status,
                        @Param("tenantId") Long tenantId,
                        @Param("adminId") Integer adminId);

        @Query("""
                            SELECT v.visitDate, COUNT(v)
                            FROM Visitor v
                            WHERE v.visitDate >= :start
                            GROUP BY v.visitDate
                            ORDER BY v.visitDate
                        """)
        List<Object[]> countDailyVisitors(@Param("start") LocalDate start);

        @Query("""
                            SELECT FUNCTION('date_trunc', 'week', v.visitDate), COUNT(v)
                            FROM Visitor v
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
                           FROM Visitor v
                           WHERE v.visitDate >= :start
                           GROUP BY YEAR(v.visitDate), MONTH(v.visitDate)
                           ORDER BY YEAR(v.visitDate), MONTH(v.visitDate)
                        """)
        List<Object[]> countMonthlyVisitors(@Param("start") LocalDate start);

}
