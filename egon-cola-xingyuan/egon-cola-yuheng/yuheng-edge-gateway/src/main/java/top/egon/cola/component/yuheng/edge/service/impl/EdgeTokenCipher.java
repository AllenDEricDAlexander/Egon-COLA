package top.egon.cola.component.yuheng.edge.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

@Component("edgeTokenCipher")
@RequiredArgsConstructor
@Slf4j
public class EdgeTokenCipher {
    private static final String PREFIX = "ENC:v1:";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final Set<PosixFilePermission> REQUIRED_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    @Qualifier("edgeOAuthProperties")
    private final EdgeOAuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile SecretKeySpec key;

    private String keyId;
    private Path keyFile;

    @PostConstruct
    public void initializeKey() {
        try {
            this.keyId = properties.getSession().getEncryption().getKeyId();
            this.keyFile = Path.of(properties.getSession().getEncryption().getKeyFile());
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(keyFile);
            byte[] bytes = Files.readAllBytes(keyFile);
            if (!permissions.equals(REQUIRED_PERMISSIONS) || bytes.length != 32) {
                throw new IllegalStateException("Edge KEK must be a 32-byte owner-only file");
            }
            this.key = new SecretKeySpec(bytes, "AES");
            log.info("Edge session encryption key initialized; keyId={}", keyId);
        } catch (Exception exception) {
            throw new IllegalStateException("Edge session encryption key is unavailable", exception);
        }
    }

    public byte[] encrypt(byte[] plaintext, String purpose, String context) {
        requireKey();
        if (plaintext == null || purpose == null || purpose.isBlank()
                || context == null || context.isBlank()) {
            throw new IllegalArgumentException("Edge encryption context is required");
        }
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(purpose, context));
            byte[] ciphertext = cipher.doFinal(plaintext);
            String envelope = PREFIX + keyId + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
            return envelope.getBytes(StandardCharsets.US_ASCII);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Edge encryption failed", exception);
        }
    }

    public byte[] decrypt(byte[] envelopeBytes, String purpose, String context) {
        requireKey();
        if (envelopeBytes == null || purpose == null || purpose.isBlank()
                || context == null || context.isBlank()) {
            throw new IllegalArgumentException("Edge decryption context is required");
        }
        String envelope = new String(envelopeBytes, StandardCharsets.US_ASCII);
        String[] parts = envelope.split(":", -1);
        if (parts.length != 5
                || !"ENC".equals(parts[0])
                || !"v1".equals(parts[1])
                || !keyId.equals(parts[2])) {
            throw new IllegalArgumentException("Edge ciphertext envelope is invalid");
        }
        try {
            byte[] nonce = Base64.getUrlDecoder().decode(parts[3]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[4]);
            if (nonce.length != NONCE_BYTES || ciphertext.length <= TAG_BITS / 8) {
                throw new IllegalArgumentException("Edge ciphertext envelope is invalid");
            }
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            if (!encoder.encodeToString(nonce).equals(parts[3])
                    || !encoder.encodeToString(ciphertext).equals(parts[4])) {
                throw new IllegalArgumentException("Edge ciphertext envelope is not canonical");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(purpose, context));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Edge ciphertext could not be authenticated", exception);
        }
    }

    private byte[] aad(String purpose, String context) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writeField(output, "edge-session-v1");
            writeField(output, keyId);
            writeField(output, purpose);
            writeField(output, context);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Edge authentication data could not be encoded", exception);
        }
        return bytes.toByteArray();
    }

    private static void writeField(DataOutputStream output, String value) throws java.io.IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private void requireKey() {
        if (key == null) {
            throw new IllegalStateException("Edge session encryption key is not initialized");
        }
    }

    @Override
    public String toString() {
        return "EdgeTokenCipher[keyId=" + keyId + ", keyFile=<redacted>]";
    }
}
