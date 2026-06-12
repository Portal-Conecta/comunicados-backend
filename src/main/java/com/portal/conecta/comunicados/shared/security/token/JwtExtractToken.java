package com.portal.conecta.comunicados.shared.security.token;

import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtExtractToken {

    @Value("${app.jwt.secret}")
    private String secret;

    public RequestContext extractUserDetails(String token) {
        Claims claims = extractClaims(token);
        UUID userId = UUID.fromString(claims.getSubject());
        UserType userType = UserType.valueOf(claims.get("userType", String.class));
        List<ContextClass> classes = extractClasses(claims.get("classes"));
        Integer permissionVersion = extractPermissionVersion(claims.get("permissionVersion"));

        return new RequestContext(userId, userType, classes, permissionVersion);
    }

    public boolean isValidToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private List<ContextClass> extractClasses(Object classesClaim) {
        if (!(classesClaim instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(this::extractClass)
                .toList();
    }

    private ContextClass extractClass(Object classClaim) {
        if (classClaim instanceof ContextClass contextClass) {
            return contextClass;
        }
        if (!(classClaim instanceof Map<?, ?> classData)) {
            throw new IllegalArgumentException("Invalid classes claim.");
        }
        Object classId = classData.get("classId");
        Object role = classData.get("role");
        if (classId == null || role == null) {
            throw new IllegalArgumentException("Invalid classes claim.");
        }
        return new ContextClass(
                UUID.fromString(classId.toString()),
                ClassRole.valueOf(role.toString())
        );
    }

    private Integer extractPermissionVersion(Object permissionVersionClaim) {
        if (permissionVersionClaim == null) {
            return null;
        }

        if (permissionVersionClaim instanceof Number number) {
            int permissionVersion = number.intValue();
            validatePermissionVersion(permissionVersion);
            return permissionVersion;
        }

        try {
            int permissionVersion = Integer.parseInt(permissionVersionClaim.toString());
            validatePermissionVersion(permissionVersion);
            return permissionVersion;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid permissionVersion claim.", exception);
        }
    }

    private void validatePermissionVersion(Integer permissionVersion) {
        if (permissionVersion < 0) {
            throw new IllegalArgumentException("Invalid permissionVersion claim.");
        }
    }

}