package top.egon.cola.platform.idp.starter.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.idp.contract.IdentityUserState;

import java.util.Objects;
import java.util.Optional;

/**
 * Reads IdP user status from the shared Redis control-plane key space.
 */
public final class RedisIdentityUserStateReader
        implements IdentityUserStateReader {

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RedisIdentityUserStateReader(
            RedissonClient redisson,
            ObjectMapper objectMapper,
            String keyPrefix
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix is required");
        }
        this.keyPrefix = keyPrefix.trim();
    }

    @Override
    public Optional<IdentityUserState> read(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IdentityStateUnavailableException("subject is required");
        }
        RBucket<String> bucket = redisson.getBucket(
                keyPrefix + subject.trim(), StringCodec.INSTANCE);
        String value = bucket.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            IdentityUserState state = objectMapper.readValue(
                    value, IdentityUserState.class);
            if (!state.subject().equals(subject.trim())) {
                throw new IdentityStateUnavailableException(
                        "identity state subject mismatch");
            }
            return Optional.of(state);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IdentityStateUnavailableException(
                    "identity state is invalid", exception);
        }
    }

    public static final class IdentityStateUnavailableException
            extends RuntimeException {

        public IdentityStateUnavailableException(String message) {
            super(message);
        }

        public IdentityStateUnavailableException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}
