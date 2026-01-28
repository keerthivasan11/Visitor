package com.smartsecurity.system.util;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.DatatypeConverter;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditLogUtil {

    // public Long getUserIdFromToken(ServletRequest servletRequest) {
    //     HttpServletRequest request = (HttpServletRequest) servletRequest;
    //     String token = request.getHeader("Authorization") != null ? request.getHeader("Authorization")
    //             : (request.getParameter("access_token") != null ? request.getParameter("access_token") : "");
    //     Claims claims = Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary("Annular"))
    //             .parseClaimsJws(token)
    //             .getBody();
    //     return Long.valueOf(claims.getId());
    // }

}
