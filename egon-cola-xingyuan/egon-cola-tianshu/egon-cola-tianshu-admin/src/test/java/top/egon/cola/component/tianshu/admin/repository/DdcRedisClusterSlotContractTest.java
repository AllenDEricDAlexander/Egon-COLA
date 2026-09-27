package top.egon.cola.component.tianshu.admin.repository;

import org.junit.jupiter.api.Test;
import org.redisson.connection.CRC16;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRedisClusterSlotContractTest {

    @Test
    void configProjectionLeaseAndLockKeysShareTheScopeSlot() {
        List<String> keys = List.of(
                DdcRedisKeys.config(101L, "retail", "dev", "demo", "switch"),
                DdcRedisKeys.version(101L, "retail", "dev", "demo", "switch"),
                DdcRedisKeys.publishIdempotency(101L,
                        "retail", "dev", "demo", "change-1"),
                DdcRedisKeys.topic(101L, "retail", "dev", "demo"),
                DdcRedisKeys.topic(101L, "retail", "dev", "demo") + ":lock",
                DdcRedisKeys.configLeaseInstance(101L,
                        "retail", "dev", "demo", "instance-1"),
                DdcRedisKeys.configLeaseInstances(101L, "retail", "dev", "demo"),
                DdcRedisKeys.configLeaseInstances(101L, "retail", "dev", "demo") + ":lock"
        );

        assertOneSlot(keys);
    }

    @Test
    void registryObjectsAndScopeLockUseOnePhysicalSlot() {
        DdcServiceKey serviceKey = new DdcServiceKey(
                "pay-biz", "dev", "orders-app", DdcServiceKind.RPC_PROVIDER,
                "order.v1.OrderQueryService", "default", "1.0.0", "grpc"
        );
        List<String> keys = List.of(
                DdcRedisKeys.registryInstance(101L, serviceKey, "provider-1"),
                DdcRedisKeys.registryService(101L, serviceKey),
                DdcRedisKeys.registryRevision(101L, serviceKey),
                DdcRedisKeys.registryCatalog(101L,
                        serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                        serviceKey.serviceKind(), serviceKey.protocol()),
                DdcRedisKeys.registryCatalogRevision(101L,
                        serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                        serviceKey.serviceKind(), serviceKey.protocol()),
                DdcRedisKeys.registryTopic(101L,
                        serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                        serviceKey.serviceKind(), serviceKey.protocol()),
                DdcRedisKeys.registryInstance(101L, serviceKey, "scope") + ":lock"
        );

        assertThat(keys).allMatch(key -> key.startsWith("tianshu:v4:tenant:101:{"));
        assertOneSlot(keys);
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
