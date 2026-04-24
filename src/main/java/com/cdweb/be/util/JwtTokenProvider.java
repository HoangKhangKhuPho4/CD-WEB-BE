package com.cdweb.be.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private int jwtExpirationInMs;

  public String generateToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

    return Jwts.builder()
        .subject(userPrincipal.getUsername())
        .issuedAt(new Date())
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public String generateTokenFromUsername(String username) {
    Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public String getUsernameFromJWT(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

    return claims.getSubject();
  }

  public boolean validateToken(String authToken) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
      return true;
    } catch (MalformedJwtException ex) {
      System.err.println("Invalid JWT token");
    } catch (ExpiredJwtException ex) {
      System.err.println("Expired JWT token");
    } catch (UnsupportedJwtException ex) {
      System.err.println("Unsupported JWT token");
    } catch (IllegalArgumentException ex) {
      System.err.println("JWT claims string is empty");
    }
    return false;
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
