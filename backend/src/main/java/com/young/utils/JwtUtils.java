package com.young.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 工具类
 */
@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    private final Key secretKey;
    // access token 有效期 2 小时，缩短泄露窗口
    private static final long EXPIRATION_TIME = 2 * 60 * 60 * 1000L;
    private static final String ISSUER = "loan-ms";
    private static final String AUDIENCE = "loan-ms-client";

    // 内存黑名单：jti -> 过期时间戳（测试环境单机方案，多实例需替换为 Redis）
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public JwtUtils(@Value("${jwt.secret}") String secretBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @param role   0-客户 1-管理员
     */
    public String generateToken(Long userId, Integer role) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        return Jwts.builder()
                .setId(jti)
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析与验证 Token，失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token 已过期");
        } catch (SignatureException e) {
            log.warn("[JWT] 签名校验失败");
        } catch (MalformedJwtException e) {
            log.warn("[JWT] Token 格式错误");
        } catch (Exception e) {
            log.warn("[JWT] Token 解析失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 将指定 jti 加入黑名单（登出/改密时调用）
     */
    public void invalidate(String jti, long expirationMillis) {
        if (jti != null) {
            blacklist.put(jti, expirationMillis);
            // 顺带清理已过期的黑名单条目
            long now = System.currentTimeMillis();
            blacklist.entrySet().removeIf(e -> e.getValue() < now);
        }
    }

    /**
     * 判断 jti 是否已被吊销
     */
    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        Long exp = blacklist.get(jti);
        if (exp == null) {
            return false;
        }
        if (exp < System.currentTimeMillis()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }
}
