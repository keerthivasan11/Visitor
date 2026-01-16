package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {
    List<Visitor> findByVisitDate(LocalDate date);

    long countByVisitDate(LocalDate date);
}
