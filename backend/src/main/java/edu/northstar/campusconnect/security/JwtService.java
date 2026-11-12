package edu.northstar.campusconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.northstar.campusconnect.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final String secret;
    private final long expirationHours;
    private final ObjectMapper objectMapper;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-hours:24}") long expirationHours,
                      ObjectMapper objectMapper) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must contain at least 32 bytes");
        }
        this.secret = secret;
        this.expirationHours = expirationHours;
        this.objectMapper = objectMapper;
    }

    public String issue(User user) {
        try {
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(objectMapper.writeValueAsBytes(Map.of(
                    "sub", user.getId().toString(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "exp", Instant.now().plusSeconds(expirationHours * 3600).getEpochSecond())));
            String content = header + "." + payload;
            return content + "." + encode(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not issue authentication token", exception);
        }
    }

    public Long verifyAndGetUserId(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return null;
            String content = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(content), Base64.getUrlDecoder().decode(parts[2]))) return null;
            Map<?, ?> claims = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            Object expiry = claims.get("exp");
            if (!(expiry instanceof Number number) || number.longValue() <= Instant.now().getEpochSecond()) return null;
            return Long.valueOf(String.valueOf(claims.get("sub")));
        } catch (Exception exception) {
            return null;
        }
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
