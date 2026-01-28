package com.smartsecurity.system.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final String SECRET = "your-secret-key";

    // public Long extractUserId(String token) {

    //     Claims claims = Jwts.parser()
    //             .setSigningKey(SECRET.getBytes())
    //             .parseClaimsJws(token)
    //             .getBody();

    //     return claims.get("userId", Long.class);
    // }
}
