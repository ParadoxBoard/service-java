package com.paradox.service_java.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class GitHubJwtGenerator {

    private static final Logger log = LoggerFactory.getLogger(GitHubJwtGenerator.class);

    private final RSAPrivateKey privateKey;
    private final String appId;

    public GitHubJwtGenerator(ResourceLoader resourceLoader,
                              @Value("${github.app.id:}") String appId,
                              @Value("${github.app.private-key-path:}") String pemPath) {
        this.appId = appId;
        RSAPrivateKey pk = null;

        if (pemPath == null || pemPath.isEmpty()) {
            log.warn("github.app.private-key-path is not set; GitHub App operations will be disabled until configured");
            this.privateKey = null;
            return;
        }

        try {
            Resource res = resourceLoader.getResource("file:" + pemPath);
            try (InputStream is = res.getInputStream()) {
                String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String priv = pem.replaceAll("-----BEGIN (.*)-----", "")
                        .replaceAll("-----END (.*)----", "")
                        .replaceAll("\\n", "").replaceAll("\\r", "")
                        .trim();
                byte[] keyBytes = Base64.getDecoder().decode(priv);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                pk = (RSAPrivateKey) kf.generatePrivate(spec);
                log.info("Loaded GitHub App private key from {}", pemPath);
            }
        } catch (Exception ex) {
            log.warn("Could not load GitHub App private key from {}: {}. GitHub App operations will be disabled until configured.", pemPath, ex.getMessage());
            pk = null;
        }
        this.privateKey = pk;
    }

    public String generateJwt() {
        if (this.privateKey == null || this.appId == null || this.appId.isEmpty()) {
            throw new IllegalStateException("GitHub App private key or app id not configured; cannot generate JWT");
        }
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusSeconds(600));

        return Jwts.builder()
                .setIssuer(appId)
                .setIssuedAt(iat)
                .setExpiration(exp)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    // Check without generating the JWT
    public boolean isConfigured() {
        return this.privateKey != null && this.appId != null && !this.appId.isEmpty();
    }

    // Expose appId for debugging (does not expose private key)
    public String getAppId() {
        return this.appId;
    }
}
