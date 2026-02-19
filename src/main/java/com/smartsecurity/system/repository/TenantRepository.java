package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Tenant;
import com.smartsecurity.system.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByCompanyName(String companyName);

    // @EntityGraph(attributePaths = "admins")
    // List<Tenant> findAll();

    @Query("SELECT DISTINCT t FROM Tenant t LEFT JOIN FETCH t.admins")
    List<Tenant> findAllWithAdmins();

    @Modifying
    @Query("UPDATE Tenant t SET t.status = :status WHERE t.id = :tenantId")
    void updateStatusByTenantId(Long tenantId, UserStatus status);

    List<Tenant> findByStatus(UserStatus status);

    long countByStatus(UserStatus status);


}
