package top.egon.cola.component.ddc.admin.security.rpc;

import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class RedisDdcNonceStore implements DdcNonceStore {

    private static final String KEY_PREFIX = "ddc:security:nonce:";

    private final RedissonClient redisson;

    public RedisDdcNonceStore(RedissonClient redisson) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
    }

    @Override
    public boolean markIfAbsent(
            String credentialId,
            String nonce,
            Duration timeToLive) {
        InMemoryDdcNonceStore.validate(
                credentialId,
                nonce,
                timeToLive
        );
        String credentialDigest = InMemoryDdcNonceStore.digest(credentialId);
        String nonceDigest = InMemoryDdcNonceStore.digest(nonce);
        String key = KEY_PREFIX + '{' + credentialDigest + "}:"
                + nonceDigest;
        return redisson.<String>getBucket(key).trySet(
                "1",
                timeToLive.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }
}
