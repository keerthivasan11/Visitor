package com.smartsecurity.system.util;
import org.springframework.stereotype.Component;
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
