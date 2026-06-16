package me.alinizamani.byline.service.impl;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import me.alinizamani.byline.domain.user.User;
import me.alinizamani.byline.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;


@Service
public class JwtServiceImpl implements JwtService {
    public static final String TYPE_ACCESS  = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.jwt-expiration-s}")
    private long accessExpirySeconds;

    @Value("${app.jwt.refresh-token-expiration-s}")
    private long refreshExpirySeconds;

    @Override
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role",  user.getRole().name())
                .claim("type",  TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(expiryFrom(accessExpirySeconds))
                .signWith(getSignInKey())
                .compact();
    }

    @Override
    public String generateRefreshToken(User user, String jti) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .id(jti)
                .claim("type", TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(expiryFrom(refreshExpirySeconds))
                .signWith(getSignInKey())
                .compact();
    }

    @Override
    public UUID extractAndValidateAccessToken(String token) {
        Claims claims = extractAllClaims(token);
        String type = claims.get("type", String.class);
        if (!TYPE_ACCESS.equals(type)) {
            throw new JwtException(
                    "Invalid token type: expected 'access', got '" + type + "'"
            );
        }
        return UUID.fromString(claims.getSubject());
    }

    @Override
    public String extractAndValidateRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        String type = claims.get("type", String.class);
        if (!TYPE_REFRESH.equals(type)) {
            throw new JwtException(
                    "Invalid token type: expected 'refresh', got '" + type + "'"
            );
        }
        return claims.getId();
    }

    @Override
    public UUID extractUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return UUID.fromString(claims.getSubject());
    }


    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey(){
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Date expiryFrom(long seconds) {
        return new Date(System.currentTimeMillis() + seconds * 1_000);
    }
}

