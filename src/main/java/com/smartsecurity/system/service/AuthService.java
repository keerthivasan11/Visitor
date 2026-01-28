package com.smartsecurity.system.service;

import com.smartsecurity.system.dto.AuthRequest;
import com.smartsecurity.system.dto.AuthResponse;

import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.security.JwtAuthenticationFilter;
import com.smartsecurity.system.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthResponse authenticate(AuthRequest request) {
                try {
                        // Sanitize email input
                        String email = request.getEmail().trim().toLowerCase();

                        // Authenticate user credentials
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(email, request.getPassword()));

                        // Retrieve user details
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> {
                                                log.warn("User not found after successful authentication: {}", email);
                                                return new UsernameNotFoundException("User not found");
                                        });

                        // Generate JWT token
                        String jwtToken = jwtService.generateToken(user);

                        log.info("User authenticated successfully: {} with role: {}", email, user.getRole());

                        return AuthResponse.builder()
                                        .token(jwtToken)
                                        .role(user.getRole())
                                        .fullName(user.getFullName())
                                        .build();

                } catch (BadCredentialsException e) {
                        log.warn("Failed login attempt for email: {} - Invalid credentials", request.getEmail());
                        throw new BadCredentialsException("Invalid email or password");
                } catch (AuthenticationException e) {
                        log.warn("Authentication failed for email: {} - {}", request.getEmail(), e.getMessage());
                        throw e;
                } catch (Exception e) {
                        log.error("Unexpected error during authentication for email: {}", request.getEmail(), e);
                        throw new RuntimeException("Authentication failed. Please try again.");
                }
        }

        public void saveFcmToken(String fcmToken) {

                User user1 = JwtAuthenticationFilter.getCurrentUser();

                User user = userRepository.findById(user1.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                user.setFcmToken(fcmToken);
                userRepository.save(user);
        }

}
