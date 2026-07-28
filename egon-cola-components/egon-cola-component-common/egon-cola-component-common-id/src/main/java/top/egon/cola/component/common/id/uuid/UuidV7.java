package top.egon.cola.component.common.id.uuid;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Compatibility helper for RFC 9562 UUID version 7 values.
 *
 * @deprecated use {@code SnowflakeIdGenerator} for new database primary keys;
 * this UUID compatibility API will be removed in the next major version
 */
@Deprecated(since = "5.3.1", forRemoval = true)
public final class UuidV7 {

    private static final long MAX_UNIX_MILLIS = (1L << 48) - 1L;
    private static final long RANDOM_B_MASK = 0x3fff_ffff_ffff_ffffL;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    /**
     * Generates a UUIDv7 using the current Unix millisecond timestamp and JDK
     * cryptographic randomness.
     *
     * @return an RFC 9562 UUID version 7 value
     */
    public static UUID generate() {
        long unixMillis = System.currentTimeMillis();
        if (unixMillis < 0L || unixMillis > MAX_UNIX_MILLIS) {
            throw new IllegalStateException("Unix timestamp cannot be represented by UUIDv7: " + unixMillis);
        }

        long randomA = RANDOM.nextInt(1 << 12);
        long randomB = RANDOM.nextLong() & RANDOM_B_MASK;
        long mostSignificantBits = (unixMillis << 16) | 0x7000L | randomA;
        long leastSignificantBits = 0x8000_0000_0000_0000L | randomB;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    /**
     * Generates a canonical 36-character UUIDv7 string.
     *
     * @return canonical UUIDv7 string
     */
    public static String string() {
        return generate().toString();
    }

    /**
     * Generates a 32-character UUIDv7 string without hyphens.
     *
     * @return compact UUIDv7 string
     */
    public static String simpleString() {
        return string().replace("-", "");
    }
}
