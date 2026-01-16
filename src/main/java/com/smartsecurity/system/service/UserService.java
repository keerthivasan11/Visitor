package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.SecurityUserRequest;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.enums.Role;
import com.smartsecurity.system.enums.UserStatus;
import com.smartsecurity.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getSecurityUsers() {
        // Simple filter based on role
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SECURITY_USER)
                .toList();
    }

    public User createSecurityUser(SecurityUserRequest request) {
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.SECURITY_USER)
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }
}
