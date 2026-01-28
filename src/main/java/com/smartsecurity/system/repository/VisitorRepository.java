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
            @Param("adminId") Integer adminId
    );
}
