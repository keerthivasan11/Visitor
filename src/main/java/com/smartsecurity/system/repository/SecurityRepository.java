package com.smartsecurity.system.repository;

import com.smartsecurity.system.entity.Security;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityRepository extends JpaRepository<Security, Long> {

    Optional<Security> findByEmail(String email);

    Optional<Security> findByMobileNumber(String mobileNumber);

    List<Security> findByStatus(com.smartsecurity.system.enums.UserStatus status);
}
