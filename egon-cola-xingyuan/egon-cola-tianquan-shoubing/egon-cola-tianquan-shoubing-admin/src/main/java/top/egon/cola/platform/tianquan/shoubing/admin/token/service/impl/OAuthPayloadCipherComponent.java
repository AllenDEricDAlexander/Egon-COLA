package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthKeyEncryptionProperties;

@Component("oauthPayloadCipherComponent")
@Slf4j
@RequiredArgsConstructor
public class OAuthPayloadCipherComponent {

    private static final int AES_KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int TAG_BYTES = TAG_BITS / Byte.SIZE;
    private static final Set<String> ALLOWED_PURPOSES = Set.of(
            "authorization-attributes", "token-value", "token-metadata", "signing-key");

    @Qualifier("oauthKeyEncryptionProperties")
    private final OAuthKeyEncryptionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    private volatile SecretKeySpec key;

    @PostConstruct
    public void initializeKey() {
        if (key != null) {
            return;
        }
        synchronized (this) {
            if (key != null) {
                return;
            }
            String keyId = properties.getKeyId();
            Path keyFile = properties.getKeyFile();
            if (keyId == null || !keyId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
                throw new IllegalStateException("OAUTH_KEK_KEY_ID_INVALID");
            }
            if (keyFile == null) {
                throw new IllegalStateException("OAUTH_KEK_FILE_REQUIRED");
            }
            if (!Files.isRegularFile(keyFile)) {
                throw new IllegalStateException("OAUTH_KEK_FILE_NOT_REGULAR");
            }
            if (!Files.isReadable(keyFile)) {
                throw new IllegalStateException("OAUTH_KEK_CONFIGURATION_INVALID");
            }
            byte[] keyBytes;
            try {
                if (!Files.getPosixFilePermissions(keyFile).equals(Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))) {
                    throw new IllegalStateException("OAUTH_KEK_FILE_PERMISSIONS_INVALID");
                }
                keyBytes = Files.readAllBytes(keyFile);
            } catch (IOException | UnsupportedOperationException failure) {
                throw new IllegalStateException("OAUTH_KEK_FILE_UNAVAILABLE", failure);
            }
            if (keyBytes.length != AES_KEY_BYTES) {
                Arrays.fill(keyBytes, (byte) 0);
                throw new IllegalStateException("OAUTH_KEK_LENGTH_INVALID");
            }
            key = new SecretKeySpec(keyBytes, "AES");
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    public String encrypt(
            TrustedRealmBO trustedRealm,
            String purpose,
            String recordKey,
            String plaintext) {
        requireContext(trustedRealm, purpose, recordKey);
        if (plaintext == null) {
            throw new IllegalArgumentException("OAUTH_PAYLOAD_PLAINTEXT_REQUIRED");
        }
        SecretKeySpec encryptionKey = requireKey();
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(associatedData(trustedRealm, purpose, recordKey));
            byte[] ciphertext = cipher.doFinal(plaintextBytes);
            return "ENC:v1:" + properties.getKeyId() + ":" + encode(nonce) + ":" + encode(ciphertext);
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("OAUTH_PAYLOAD_ENCRYPTION_FAILED", failure);
        } finally {
            Arrays.fill(plaintextBytes, (byte) 0);
        }
    }

    public String decrypt(
            TrustedRealmBO trustedRealm,
            String purpose,
            String recordKey,
            String envelope) {
        requireContext(trustedRealm, purpose, recordKey);
        if (envelope == null) {
            throw new IllegalArgumentException("OAUTH_PAYLOAD_ENVELOPE_REQUIRED");
        }
        SecretKeySpec encryptionKey = requireKey();
        String[] parts = envelope.split(":", -1);
        if (parts.length != 5 || !"ENC".equals(parts[0]) || !"v1".equals(parts[1])
                || !properties.getKeyId().equals(parts[2])) {
            throw new IllegalArgumentException("OAUTH_PAYLOAD_ENVELOPE_UNSUPPORTED");
        }
        try {
            byte[] nonce = Base64.getUrlDecoder().decode(parts[3]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[4]);
            if (nonce.length != NONCE_BYTES || ciphertext.length < TAG_BYTES) {
                throw new IllegalArgumentException("OAUTH_PAYLOAD_ENVELOPE_INVALID");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(associatedData(trustedRealm, purpose, recordKey));
            byte[] plaintext = cipher.doFinal(ciphertext);
            try {
                return new String(plaintext, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(plaintext, (byte) 0);
            }
        } catch (AEADBadTagException failure) {
            throw new IllegalArgumentException("OAUTH_PAYLOAD_AUTHENTICATION_FAILED", failure);
        } catch (IllegalArgumentException failure) {
            if (failure.getMessage() != null && failure.getMessage().startsWith("OAUTH_PAYLOAD_")) {
                throw failure;
            }
            throw new IllegalArgumentException("OAUTH_PAYLOAD_ENVELOPE_INVALID", failure);
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("OAUTH_PAYLOAD_DECRYPTION_FAILED", failure);
        }
    }

    private SecretKeySpec requireKey() {
        SecretKeySpec current = key;
        if (current == null) {
            throw new IllegalStateException("OAUTH_KEK_NOT_INITIALIZED");
        }
        return current;
    }

    private static void requireContext(TrustedRealmBO realm, String purpose, String recordKey) {
        if (realm == null) {
            throw new IllegalArgumentException("TRUSTED_REALM_REQUIRED_FOR_OAUTH_PAYLOAD_CIPHER");
        }
        realm.validateTrustedPartition();
        if (purpose == null || !ALLOWED_PURPOSES.contains(purpose)
                || recordKey == null || recordKey.isBlank() || !recordKey.equals(recordKey.trim())
                || recordKey.length() > 256) {
            throw new IllegalArgumentException("OAUTH_PAYLOAD_CONTEXT_INVALID");
        }
    }

    private static byte[] associatedData(TrustedRealmBO realm, String purpose, String recordKey) {
        String[] values = {realm.getIssuer().normalize().toASCIIString(), realm.getRealmKey(), purpose, recordKey};
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                for (String value : values) {
                    byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
                    output.writeInt(encoded.length);
                    output.write(encoded);
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("OAUTH_PAYLOAD_AAD_ENCODING_FAILED", impossible);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
