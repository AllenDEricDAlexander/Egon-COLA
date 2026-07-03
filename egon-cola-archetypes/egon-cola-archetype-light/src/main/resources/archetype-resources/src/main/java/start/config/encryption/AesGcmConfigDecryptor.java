package ${package}.start.config.encryption;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmConfigDecryptor implements ConfigDecryptor {

    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";
    private static final String VERSION = "v1";
    private static final int GCM_TAG_BITS = 128;

    @Override
    public boolean supports(String value) {
        return value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX);
    }

    @Override
    public String decrypt(String value, char[] key) {
        if (!supports(value)) {
            return value;
        }
        Objects.requireNonNull(key, "key must not be null");
        String[] parts = value.substring(PREFIX.length(), value.length() - SUFFIX.length()).split(":");
        if (parts.length != 4 || !VERSION.equals(parts[0])) {
            throw new ConfigDecryptException("Invalid encrypted configuration value format");
        }
        byte[] keyBytes = null;
        try {
            keyBytes = secretKeyBytes(key);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getDecoder().decode(parts[2]);
            byte[] tag = Base64.getDecoder().decode(parts[3]);
            byte[] encrypted = new byte[cipherText.length + tag.length];
            System.arraycopy(cipherText, 0, encrypted, 0, cipherText.length);
            System.arraycopy(tag, 0, encrypted, cipherText.length, tag.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(keyBytes), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new ConfigDecryptException("Failed to decrypt encrypted configuration value", ex);
        } finally {
            if (keyBytes != null) {
                Arrays.fill(keyBytes, (byte) 0);
            }
        }
    }

    private byte[] secretKeyBytes(char[] key) {
        ByteBuffer keyBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(key));
        byte[] bytes = new byte[keyBuffer.remaining()];
        keyBuffer.get(bytes);
        if (keyBuffer.hasArray()) {
            Arrays.fill(keyBuffer.array(), (byte) 0);
        }
        if (bytes.length != 32) {
            Arrays.fill(bytes, (byte) 0);
            throw new ConfigDecryptException("EGON_CONFIG_DECRYPT_KEY must be 32 UTF-8 bytes for AES-256-GCM");
        }
        return bytes;
    }

    private SecretKey secretKey(byte[] bytes) {
        return new SecretKeySpec(bytes, "AES");
    }
}
