package top.egon.cola.component.bytecode.core.enhance;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MethodId {

    private MethodId() {
    }

    public static long compute(String owner, String methodName, String methodDescriptor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, owner);
            update(digest, methodName);
            update(digest, methodDescriptor);
            return ByteBuffer.wrap(digest.digest()).getLong();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
