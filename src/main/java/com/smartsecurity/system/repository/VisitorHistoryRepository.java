package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Visitor;
import com.smartsecurity.system.entity.VisitorHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitorHistoryRepository extends JpaRepository<VisitorHistory, Long> {
    List<VisitorHistory> findByVisitor(Visitor visitor);

    List<VisitorHistory> findByVisitorId(Long visitorId);

    Optional<VisitorHistory> findByVisitorAndCheckOutTimeIsNull(Visitor visitor);

    @org.springframework.data.jpa.repository.Query("SELECT vh FROM VisitorHistory vh WHERE " +
            "(:tenantId IS NULL OR (vh.tenant IS NOT NULL AND vh.tenant.id = :tenantId)) AND " +
            "vh.visitDate BETWEEN :start AND :end")
    List<VisitorHistory> findByFilters(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
