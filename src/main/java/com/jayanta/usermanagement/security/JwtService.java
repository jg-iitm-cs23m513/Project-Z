package com.jayanta.usermanagement.security;

import com.jayanta.usermanagement.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT Service - Token Issuer (RS256 with encrypted RSA-4096 private key)
 *
 * Signs JWT tokens using an AES-256-CBC encrypted RSA-4096 private key.
 * Verifies tokens using the corresponding RSA public key.
 * Embeds license expiration in token claims for stateless cross-service validation.
 */
@Service
@Slf4j
public class JwtService {

    /*
     * Register BouncyCastle as a JCA security provider.
     */
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${jwt.private-key-passphrase}")
    private String privateKeyPassphrase;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    // ─── Key Loading ────────────────────────────────────────────────────────────

    /**
     * Load the RSA private key from PEM file.
     * Handles:
     *   - BEGIN ENCRYPTED PRIVATE KEY  (PKCS#8 encrypted — decrypted with passphrase)
     *   - BEGIN PRIVATE KEY            (PKCS#8 unencrypted)
     *   - BEGIN RSA PRIVATE KEY        (PKCS#1 unencrypted)
     */
    private PrivateKey loadPrivateKey() {
        try (PEMParser parser = new PEMParser(new InputStreamReader(
                resolveKeyPath(privateKeyPath), StandardCharsets.UTF_8))) {

            Object pem = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (pem instanceof PKCS8EncryptedPrivateKeyInfo encrypted) {
                PrivateKeyInfo decrypted = encrypted.decryptPrivateKeyInfo(
                        new JcePKCSPBEInputDecryptorProviderBuilder()
                                .setProvider("BC")
                                .build(privateKeyPassphrase.toCharArray()));
                return converter.getPrivateKey(decrypted);
            }
            if (pem instanceof PrivateKeyInfo info) {
                return converter.getPrivateKey(info);
            }
            if (pem instanceof PEMKeyPair pair) {
                return converter.getPrivateKey(pair.getPrivateKeyInfo());
            }

            throw new IllegalStateException("Unknown PEM private key type: " + pem.getClass().getName());
        } catch (Exception e) {
            log.error("Failed to load private key: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load RSA private key", e);
        }
    }

    /**
     * Load the RSA public key from PEM file.
     * Handles:
     *   - BEGIN PUBLIC KEY       (X.509/SPKI)
     *   - BEGIN RSA PUBLIC KEY   (PKCS#1)
     *   - BEGIN RSA PRIVATE KEY  (extracts public component from key pair)
     */
    private PublicKey loadPublicKey() {
        try (PEMParser parser = new PEMParser(new InputStreamReader(
                resolveKeyPath(publicKeyPath), StandardCharsets.UTF_8))) {

            Object pem = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (pem instanceof SubjectPublicKeyInfo spki) {
                return converter.getPublicKey(spki);
            }
            if (pem instanceof PEMKeyPair pair) {
                return converter.getPublicKey(pair.getPublicKeyInfo());
            }

            throw new IllegalStateException("Unknown PEM public key type: " + pem.getClass().getName());
        } catch (Exception e) {
            log.error("Failed to load public key: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load RSA public key", e);
        }
    }

    /**
     * Resolve a key path — supports classpath: prefix or file system paths.
     */
    private InputStream resolveKeyPath(String path) {
        try {
            if (path.startsWith("classpath:")) {
                ResourceLoader loader = new DefaultResourceLoader();
                return loader.getResource(path).getInputStream();
            }
            return Files.newInputStream(Paths.get(path));
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve key path: " + path, e);
        }
    }

    // ─── Token Generation ───────────────────────────────────────────────────────

    /**
     * Generate a JWT token for the given user.
     * Includes roles, isInternal flag, and licenseExpiredOn for cross-service validation.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Add roles
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", roles);

        // Add license claims if the UserDetails is an AppUser
        if (userDetails instanceof AppUser appUser) {
            claims.put("isInternal", appUser.getIsInternal());
            if (appUser.getLicenseExpiredOn() != null) {
                claims.put("licenseExpiredOn",
                        appUser.getLicenseExpiredOn().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        PrivateKey privateKey = loadPrivateKey();

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(privateKey)
                .compact();
    }

    // ─── Token Parsing & Validation ─────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(loadPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Check if the license is valid based on token claims.
     * Internal users and Admins bypass the license check.
     */
    private boolean isLicenseValid(String token) {
        Claims claims = extractAllClaims(token);

        Boolean isInternal = claims.get("isInternal", Boolean.class);
        if (Boolean.TRUE.equals(isInternal)) {
            return true; // Internal users bypass license check
        }

        // Check roles — Admins bypass license check
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        if (roles != null && roles.contains("ROLE_Admin")) {
            return true;
        }

        String licenseExpStr = claims.get("licenseExpiredOn", String.class);
        if (licenseExpStr == null) {
            return false; // No license = denied
        }

        LocalDateTime licenseExpiry = LocalDateTime.parse(licenseExpStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return licenseExpiry.isAfter(LocalDateTime.now());
    }

    /**
     * Validate token: checks username match, token expiration, and license expiration.
     */
    public Boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && isLicenseValid(token));
    }
}