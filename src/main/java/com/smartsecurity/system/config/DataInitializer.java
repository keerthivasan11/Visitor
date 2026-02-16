package com.smartsecurity.system.config;

import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.enums.Role;
import com.smartsecurity.system.enums.UserStatus;
import com.smartsecurity.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@tower.com").isEmpty()) {
            User superAdmin = User.builder()
                    .fullName("Super Admin")
                    .email("admin@tower.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.SUPER_ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(superAdmin);
        }
    }
}
