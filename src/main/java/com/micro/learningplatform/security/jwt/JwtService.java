package com.micro.learningplatform.security.jwt;

import com.micro.learningplatform.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    /*
     * Ovo je kljucna klasa koja kreira i radi s tokenima
     * Osigurava da su tokeni validini da svaki token ima dobar potpis
     * - tskoder za generaciju i stvaranje tokena i refresh tokena
     * te sigurnostnih kljucvea koji se krairau za vrijeme pokretanja
     */

    private static final Logger log = LogManager.getLogger(JwtService.class);
    
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.access-token.expiration}")
    private long accessTokenExpiration;

    @Value("${security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    private Key signingKey;

    public LocalDateTime getAccessTokenExpiration(LocalDateTime issuedAt) {
        return issuedAt.plusSeconds(accessTokenExpiration / 1000); // Pretvaramo milisekunde u sekunde
    }

    public LocalDateTime getRefreshTokenExpiration(LocalDateTime issuedAt) {
        return issuedAt.plusSeconds(refreshTokenExpiration / 1000);
    }

    // Inicijaliziramo signing key pri pokretanju servisa
    // prakitcki secret key je jedinstvne te propustamo pristup samo valjanim kljucevima
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // metod akoje generira novi token prema pomocnoj metodi buildtoken
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();
        return buildToken(extraClaims, userDetails, getAccessTokenExpiration(now));
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();
        return buildToken(new HashMap<>(), userDetails, getRefreshTokenExpiration(now));
    }

    // dodajmo osnovne imformacije o korisniku, i dodatne ako si potreben imamo vremenski "zig" i vrijeme istjecanja
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            LocalDateTime expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        log.info("isValid token {} ", username);
        log.info("isValid token extractUserName {} ", extractUsername(token));
        log.debug("Provjera korisnika iz baze: {}", userDetails.getUsername());
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // TODO razmisliti ocu li dodati attributes i za jwt ili samo za o2auth
    public Map<String, Object> generateCustomClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
       // claims.put("attributes", user.getAttributes());
        claims.put("userId", user.getId().toString());
        claims.put("roles", user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        return claims;
    }

}
