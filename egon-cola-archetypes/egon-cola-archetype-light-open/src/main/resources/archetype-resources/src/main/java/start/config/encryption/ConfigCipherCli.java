package ${package}.start.config.encryption;

import java.io.Console;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class ConfigCipherCli {

    private static final String USAGE = "Usage: ConfigCipherCli < plaintext"
            + System.lineSeparator()
            + "Set EGON_CONFIG_DECRYPT_KEY or EGON_CONFIG_DECRYPT_KEY_FILE before running.";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ConfigCipherCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            throw new IllegalArgumentException(USAGE);
        }
        char[] key = new ConfigDecryptKeyProvider()
                .resolveKey()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing EGON_CONFIG_DECRYPT_KEY or EGON_CONFIG_DECRYPT_KEY_FILE"));
        try {
            String plainText = readPlainText();
            System.out.println(encrypt(key, plainText));
        } finally {
            Arrays.fill(key, '\0');
        }
    }

    static String encrypt(char[] key, String plainText) throws Exception {
        byte[] keyBytes = secretKeyBytes(key);
        byte[] iv = new byte[12];
        SECURE_RANDOM.nextBytes(iv);
        byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = null;
        byte[] cipherText = null;
        byte[] tag = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(128, iv));
            encrypted = cipher.doFinal(plainTextBytes);
            cipherText = Arrays.copyOf(encrypted, encrypted.length - 16);
            tag = Arrays.copyOfRange(encrypted, encrypted.length - 16, encrypted.length);
            return "ENC(v1:%s:%s:%s)".formatted(
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(cipherText),
                    Base64.getEncoder().encodeToString(tag)
            );
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
            Arrays.fill(plainTextBytes, (byte) 0);
            if (encrypted != null) {
                Arrays.fill(encrypted, (byte) 0);
            }
            if (cipherText != null) {
                Arrays.fill(cipherText, (byte) 0);
            }
            if (tag != null) {
                Arrays.fill(tag, (byte) 0);
            }
        }
    }

    private static byte[] secretKeyBytes(char[] key) {
        ByteBuffer keyBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(key));
        byte[] bytes = new byte[keyBuffer.remaining()];
        keyBuffer.get(bytes);
        if (keyBuffer.hasArray()) {
            Arrays.fill(keyBuffer.array(), (byte) 0);
        }
        if (bytes.length != 32) {
            Arrays.fill(bytes, (byte) 0);
            throw new IllegalArgumentException("key must be 32 UTF-8 bytes");
        }
        return bytes;
    }

    private static String readPlainText() throws IOException {
        Console console = System.console();
        if (console != null) {
            char[] plainText = console.readPassword("Plaintext: ");
            try {
                return new String(plainText);
            } finally {
                Arrays.fill(plainText, '\0');
            }
        }
        byte[] plainText = System.in.readAllBytes();
        try {
            return stripOneTerminalLineEnding(new String(plainText, StandardCharsets.UTF_8));
        } finally {
            Arrays.fill(plainText, (byte) 0);
        }
    }

    static String stripOneTerminalLineEnding(String value) {
        if (value.endsWith("\r\n")) {
            return value.substring(0, value.length() - 2);
        }
        if (value.endsWith("\n") || value.endsWith("\r")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
