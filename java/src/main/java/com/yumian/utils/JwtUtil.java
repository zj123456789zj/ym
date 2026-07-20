package com.yumian.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类
 * 使用 java-jwt 库生成和验证 token
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private long expire;

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @return JWT token 字符串
     */
    public String generateToken(Long userId) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire);

        return JWT.create()
                .withClaim("userId", userId)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    /**
     * 从 Token 中提取 userId
     *
     * @param token JWT token
     * @return userId，如果解析失败返回 null
     */
    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getClaim("userId").asLong();
        } catch (JWTDecodeException e) {
            log.warn("JWT token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT token
     * @return true 如果 token 有效
     */
    public boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("JWT token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     *
     * @param token JWT token
     * @return true 如果 token 已过期
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            Date expiresAt = decodedJWT.getExpiresAt();
            return expiresAt != null && expiresAt.before(new Date());
        } catch (JWTDecodeException e) {
            log.warn("JWT token 解析失败: {}", e.getMessage());
            return true;
        }
    }
}
