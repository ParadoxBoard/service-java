package com.paradox.service_java.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

    private final RSAPrivateKey privateKey;
    private final String appId;

    public GitHubJwtGenerator(ResourceLoader resourceLoader,
                              @Value("${github.app.id:}") String appId,
                              @Value("${github.app.private-key-path:}") String pemPath) throws Exception {
        this.appId = appId;
        if (pemPath == null || pemPath.isEmpty()) {
            throw new IllegalStateException("github.app.private-key-path not set");
        }
        Resource res = resourceLoader.getResource("file:" + pemPath);
        try (InputStream is = res.getInputStream()) {
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String priv = pem.replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)----", "")
                    .replaceAll("\n", "").replaceAll("\r", "")
                    .trim();
            byte[] keyBytes = Base64.getDecoder().decode(priv);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = (RSAPrivateKey) kf.generatePrivate(spec);
        }
    }

    public String generateJwt() {
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
}
