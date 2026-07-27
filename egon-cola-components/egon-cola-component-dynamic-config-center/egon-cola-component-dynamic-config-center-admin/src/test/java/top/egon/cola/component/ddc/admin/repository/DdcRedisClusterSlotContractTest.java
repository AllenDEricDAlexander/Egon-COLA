package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.connection.CRC16;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcRedisClusterSlotContractTest {

    private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

    @Test
    void configKeysAndLeaseScriptKeysShareTheScopeSlot() {
        List<String> projectionKeys = List.of(
                DdcKeys.v2Config("demo", "dev", "default", "switch"),
                DdcKeys.v2Version("demo", "dev", "default", "switch"),
                DdcKeys.v2PublishIdempotency(
                        "demo", "dev", "default", "change-1"
                ),
                DdcKeys.v2Topic("demo", "dev", "default")
        );
        AtomicReference<List<Object>> scriptKeys = new AtomicReference<>();
        RedissonClient redisson = scriptingClient(scriptKeys, 1L);
        DdcConfigLeaseRedisRepository repository =
                new DdcConfigLeaseRedisRepository(redisson, new ObjectMapper());

        repository.register(
                new DdcInstanceIdentity(
                        "instance-1", "demo", "dev", "default",
                        "127.0.0.1", 8080, "100", "5.2.3"
                ),
                new DdcLeaseSession(
                        "instance-1", "lease-1", DdcLeaseRole.CONFIG_CLIENT,
                        30, 10, NOW, NOW.plusSeconds(30)
                ),
                NOW
        );

        assertOneSlot(projectionKeys);
        assertOneSlot(scriptKeys.get().stream().map(Object::toString).toList());
        assertThat(slot(scriptKeys.get().getFirst().toString()))
                .isEqualTo(slot(projectionKeys.getFirst()));
    }

    @Test
    void registryScriptUsesOnlyV2KeysInOneKindScopeSlot() {
        AtomicReference<List<Object>> scriptKeys = new AtomicReference<>();
        AtomicReference<Object[]> arguments = new AtomicReference<>();
        RedissonClient redisson = scriptingClient(scriptKeys, List.of(1L, 1L, 1L), arguments);
        DdcServiceRegistryRedisRepository repository =
                new DdcServiceRegistryRedisRepository(
                        redisson,
                        new ObjectMapper().registerModule(new JavaTimeModule())
                );

        repository.register(instance());

        List<String> keys = scriptKeys.get().stream().map(Object::toString).toList();
        assertThat(keys).hasSize(6).allMatch(key -> key.startsWith("ddc:v2:{"));
        assertOneSlot(keys);
        assertThat(arguments.get())
                .contains(DdcKeys.registryTopic(
                        "dev", "default", DdcServiceKind.RPC_PROVIDER, "grpc"
                ));
        assertThat(keys).doesNotContain(DdcKeys.registryTopic(
                "dev", "default", DdcServiceKind.RPC_PROVIDER, "grpc"
        ));
    }

    private RedissonClient scriptingClient(AtomicReference<List<Object>> scriptKeys,
                                            Object result) {
        return scriptingClient(scriptKeys, result, new AtomicReference<>());
    }

    private RedissonClient scriptingClient(AtomicReference<List<Object>> scriptKeys,
                                            Object result,
                                            AtomicReference<Object[]> arguments) {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                any(RScript.ReturnType.class),
                anyList(),
                any(Object[].class)
        )).thenAnswer(invocation -> {
            scriptKeys.set(invocation.getArgument(3));
            arguments.set(invocation.getArguments());
            return result;
        });
        return redisson;
    }

    private DdcServiceInstance instance() {
        DdcServiceKey serviceKey = new DdcServiceKey(
                "dev",
                "default",
                DdcServiceKind.RPC_PROVIDER,
                "order.v1.OrderQueryService",
                "default",
                "1.0.0",
                "grpc"
        );
        return new DdcServiceInstance(
                "provider-1",
                "lease-1",
                serviceKey,
                "127.0.0.1",
                19090,
                false,
                Map.of("zone", "east"),
                30,
                10,
                NOW,
                NOW,
                NOW.plusSeconds(30),
                "ONLINE",
                0L
        );
    }

    private void assertOneSlot(List<String> keys) {
        assertThat(keys).extracting(this::slot).containsOnly(slot(keys.getFirst()));
    }

    private int slot(String key) {
        int open = key.indexOf('{');
        int close = open < 0 ? -1 : key.indexOf('}', open + 1);
        String slotInput = open >= 0 && close > open + 1
                ? key.substring(open + 1, close)
                : key;
        return CRC16.crc16(slotInput.getBytes(StandardCharsets.UTF_8)) % 16384;
    }
}
