package top.egon.cola.component.gateway.engine.traffic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RedissonRedisTokenBucketExecutorTest {

    @Test
    void requiresDedicatedRedissonClient() {
        assertThrows(
                NullPointerException.class,
                () -> new RedissonRedisTokenBucketExecutor(null)
        );
    }
}
