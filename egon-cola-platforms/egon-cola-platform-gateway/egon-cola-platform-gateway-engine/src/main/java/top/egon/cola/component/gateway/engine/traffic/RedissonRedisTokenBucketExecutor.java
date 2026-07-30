package top.egon.cola.component.gateway.engine.traffic;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.List;
import java.util.Objects;

public final class RedissonRedisTokenBucketExecutor
        implements RedisTokenBucketExecutor {

    private final RedissonClient redisson;

    public RedissonRedisTokenBucketExecutor(RedissonClient redisson) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
    }

    @Override
    public List<Long> execute(
            String script,
            List<String> keys,
            List<String> arguments) {
        List<?> result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.MULTI,
                keys.stream().map(value -> (Object) value).toList(),
                arguments.toArray()
        );
        if (result == null) {
            return null;
        }
        return result.stream()
                .map(value -> value instanceof Number number
                        ? number.longValue()
                        : Long.parseLong(value.toString()))
                .toList();
    }
}
