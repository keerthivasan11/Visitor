package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.enums.Role;
import com.smartsecurity.system.enums.UserStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByTenantId(Long tenantId);

    User findUserById(Integer userId);

    boolean existsByEmail(String email);

    List<User> findByTenant_IdIn(List<Long> tenantIds);

    List<User> findByTenant_Id(Long tenantId);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.tenant.id = :tenantId")
    void updateStatusByTenantId(Long tenantId, UserStatus status);

}
