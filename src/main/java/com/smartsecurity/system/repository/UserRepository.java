package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByTenantId(Long tenantId);

    User findUserById(Integer userId);

     boolean existsByEmail(String email);
}
