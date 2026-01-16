package com.smartsecurity.system.util;

import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditLogUtil {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            String email = jwtService.extractUsername(token);
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
