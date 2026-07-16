package top.egon.cola.component.bytecode.core.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record ClassMetadataCacheKey(String digest) {

    public static ClassMetadataCacheKey create(
            byte[] classBytes,
            String parserSchema,
            String asmBaseline,
            String scanConfigurationDigest
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, parserSchema);
            update(digest, asmBaseline);
            update(digest, scanConfigurationDigest);
            digest.update(classBytes);
            return new ClassMetadataCacheKey(HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JDK", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
