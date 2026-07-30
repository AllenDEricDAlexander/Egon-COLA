package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.model.dto.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcServiceRegistryRedisRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void registrationPassesLuaEpochFieldsAndMapsInstanceConflicts() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        AtomicReference<Object[]> arguments = new AtomicReference<>();
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.MULTI),
                anyList(),
                any(Object[].class)
        )).thenAnswer(invocation -> {
            arguments.set(invocation.getArguments());
            return List.of(1L, 1L, 1L);
        });
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DdcServiceRegistryRedisRepository repository =
                new DdcServiceRegistryRedisRepository(redisson, objectMapper);

        repository.register(instance());

        JsonNode stored = objectMapper.readTree((String) arguments.get()[7]);
        assertThat(stored.path("serviceKeyCanonical").asText())
                .isEqualTo(SERVICE_KEY.canonicalValue());
        assertThat(stored.path("lastHeartbeatAtEpochMillis").asLong())
                .isEqualTo(NOW.toEpochMilli());
        assertThat(stored.path("leaseExpireAtEpochMillis").asLong())
                .isEqualTo(NOW.plusSeconds(30).toEpochMilli());

        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.MULTI),
                anyList(),
                any(Object[].class)
        )).thenReturn(List.of(2L, 0L, 0L));

        assertThatThrownBy(() -> repository.register(instance()))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("instance id conflict");
    }

    @Test
    void heartbeatAndDeregisterPreserveStrictLeaseOutcomes() {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.MULTI),
                anyList(),
                any(Object[].class)
        )).thenReturn(List.of(3L, 0L));
        DdcServiceRegistryRedisRepository repository =
                new DdcServiceRegistryRedisRepository(
                        redisson,
                        new ObjectMapper().registerModule(new JavaTimeModule())
                );

        assertThat(repository.heartbeat(leaseRequest(), NOW).status())
                .isEqualTo(DdcLeaseOperationStatus.LEASE_MISMATCH);
        assertThat(repository.deregister(leaseRequest(), NOW).status())
                .isEqualTo(DdcLeaseOperationStatus.NOT_DELETED);
    }

    private DdcServiceInstance instance() {
        return new DdcServiceInstance(
                "provider-1",
                "lease-1",
                SERVICE_KEY,
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

    private DdcServiceLeaseRequest leaseRequest() {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setEnv(SERVICE_KEY.env());
        request.setNamespace(SERVICE_KEY.namespace());
        request.setServiceKind(SERVICE_KEY.serviceKind());
        request.setServiceKey(SERVICE_KEY);
        request.setInstanceId("provider-1");
        request.setLeaseId("lease-1");
        return request;
    }

    private static final DdcServiceKey SERVICE_KEY = new DdcServiceKey(
            "dev",
            "default",
            DdcServiceKind.RPC_PROVIDER,
            "order.v1.OrderQueryService",
            "default",
            "1.0.0",
            "grpc"
    );
}
