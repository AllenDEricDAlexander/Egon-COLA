package top.egon.cola.component.common.id.uuid;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * UUIDv7 generation helper.
 */
public final class UuidV7 {

    private UuidV7() {
    }

    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    public static String string() {
        return generate().toString();
    }

    public static String simpleString() {
        return string().replace("-", "");
    }
}
