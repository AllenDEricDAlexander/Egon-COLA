package top.egon.cola.component.gateway.engine.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisMcpSessionStoreTest {

    @Test
    void initialStreamReadUsesAValidExclusiveRedisOffset() {
        assertEquals(
                "0-0",
                RedisMcpSessionStore.initialReadOffset().toString()
        );
    }
}
