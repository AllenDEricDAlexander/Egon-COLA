package top.egon.cola.component.gateway.engine.common.traffic.adapter;

import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedissonRedisTokenBucketExecutorTest {

    @Test
    void requiresDedicatedRedissonClient() {
        assertThrows(
                NullPointerException.class,
                () -> new RedissonRedisTokenBucketExecutor(null)
        );
    }

    @Test
    void executesNumericLuaArgumentsWithStringCodec() {
        AtomicReference<Object> codec = new AtomicReference<>();
        RScript script = (RScript) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{RScript.class},
                (proxy, method, arguments) -> method.getName().equals("eval")
                        ? List.of(1L, 0L, 0L, 1000L)
                        : null
        );
        RedissonClient redisson = (RedissonClient) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{RedissonClient.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getScript")) {
                        codec.set(arguments == null ? null : arguments[0]);
                        return script;
                    }
                    return null;
                }
        );
        RedissonRedisTokenBucketExecutor executor =
                new RedissonRedisTokenBucketExecutor(redisson);

        List<Long> result = executor.execute(
                "return {ARGV[1]}",
                List.of("gateway:ratelimit:test"),
                List.of("1")
        );

        assertSame(StringCodec.INSTANCE, codec.get());
        assertEquals(List.of(1L, 0L, 0L, 1000L), result);
    }
}
