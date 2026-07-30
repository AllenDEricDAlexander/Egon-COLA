package top.egon.cola.component.gateway.admin.infrastructure.security;

import top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public final class AesGcmGatewaySecretProtector
        implements GatewaySecretProtector {

    private static final int IV_BYTES = 12;

    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;

    private final String keyVersion;

    private final SecureRandom random;

    public AesGcmGatewaySecretProtector(
            byte[] key,
            String keyVersion) {
        this(key, keyVersion, new SecureRandom());
    }

    AesGcmGatewaySecretProtector(
            byte[] key,
            String keyVersion,
            SecureRandom random) {
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException(
                    "AES-256-GCM key must contain 32 bytes"
            );
        }
        if (keyVersion == null || keyVersion.isBlank()) {
            throw new IllegalArgumentException("keyVersion is required");
        }
        this.key = new SecretKeySpec(key.clone(), "AES");
        this.keyVersion = keyVersion.trim();
        this.random = random;
    }

    @Override
    public ProtectedSecret protect(
            String plaintext,
            String associatedData) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_BITS, iv)
            );
            cipher.updateAAD(aad(associatedData));
            byte[] encrypted = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );
            return new ProtectedSecret(
                    Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(iv)
                            + "."
                            + Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(encrypted),
                    keyVersion
            );
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException(
                    "gateway secret encryption failed",
                    failure
            );
        }
    }

    @Override
    public String unprotect(
            ProtectedSecret secret,
            String associatedData) {
        if (!keyVersion.equals(secret.keyVersion())) {
            throw new IllegalArgumentException(
                    "gateway secret key version is not available"
            );
        }
        String[] parts = secret.ciphertext().split("\\.", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "gateway secret ciphertext is malformed"
            );
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(
                            TAG_BITS,
                            Base64.getUrlDecoder().decode(parts[0])
                    )
            );
            cipher.updateAAD(aad(associatedData));
            return new String(
                    cipher.doFinal(
                            Base64.getUrlDecoder().decode(parts[1])
                    ),
                    StandardCharsets.UTF_8
            );
        } catch (GeneralSecurityException | IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "gateway secret cannot be decrypted",
                    failure
            );
        }
    }

    private byte[] aad(String associatedData) {
        if (associatedData == null || associatedData.isBlank()) {
            throw new IllegalArgumentException(
                    "associatedData is required"
            );
        }
        return associatedData.getBytes(StandardCharsets.UTF_8);
    }
}
