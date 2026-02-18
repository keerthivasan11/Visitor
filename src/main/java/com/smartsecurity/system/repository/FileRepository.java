package com.smartsecurity.system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartsecurity.system.entity.File;
import com.smartsecurity.system.entity.Visitor;
import com.smartsecurity.system.enums.UserStatus;

public interface FileRepository extends JpaRepository<File, Long> {

   Optional<File> findByVisitor(Visitor visitor);

   Optional<File> findByVisitor_Id(Long visitorId);

   void deleteByVisitor_Id(Long visitorId);

   @Modifying
   @Query("UPDATE File f SET f.status = :status WHERE f.visitor.tenant.id = :tenantId")
   void updateStatusByTenantId(Long tenantId, UserStatus status);

   @Modifying
   @Query("UPDATE File f SET f.status = :status WHERE f.visitor.id = :visitorId")
   void updateStatusByVisitorId(@Param("visitorId") Long visitorId,
         @Param("status") UserStatus status);

}
