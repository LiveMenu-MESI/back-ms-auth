package cloudSecurity.util;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Anonymizes IP addresses for analytics (enunciado: IP_Hash anonimizado).
 * Uses SHA-256(salt + ip) so the same IP always produces the same hash
 * for unique-visitor counting, but the original IP cannot be recovered.
 */
@ApplicationScoped
public class IpAnonymization {

    private static final String ALGORITHM = "SHA-256";

    @ConfigProperty(name = "app.analytics.ip-hash.salt", defaultValue = "livemenu-default-salt")
    String salt;

    /**
     * Returns a one-way hash of the IP address for storage in analytics.
     * Returns null if ip is null or blank.
     */
    public String hash(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            String input = salt + ":" + ip.trim();
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
