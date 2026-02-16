package com.smartsecurity.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.smartsecurity.system.entity.RefreshToken;
import com.smartsecurity.system.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteAllByUser(User user);
}
