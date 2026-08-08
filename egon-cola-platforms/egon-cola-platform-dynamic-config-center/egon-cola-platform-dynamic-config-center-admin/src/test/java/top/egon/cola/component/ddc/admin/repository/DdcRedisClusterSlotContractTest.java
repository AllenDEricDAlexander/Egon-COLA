package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.redisson.connection.CRC16;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRedisClusterSlotContractTest {

    @Test
    void configProjectionLeaseAndLockKeysShareTheScopeSlot() {
        List<String> keys = List.of(
                DdcKeys.config("retail", "dev", "demo", "switch"),
                DdcKeys.version("retail", "dev", "demo", "switch"),
                DdcKeys.publishIdempotency(
                        "retail", "dev", "demo", "change-1"),
                DdcKeys.topic("retail", "dev", "demo"),
                DdcKeys.topic("retail", "dev", "demo") + ":lock",
                DdcKeys.configLeaseInstance(
                        "retail", "dev", "demo", "instance-1"),
                DdcKeys.configLeaseInstances("retail", "dev", "demo"),
                DdcKeys.configLeaseInstances("retail", "dev", "demo") + ":lock"
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
                DdcKeys.registryInstance(serviceKey, "provider-1"),
                DdcKeys.registryService(serviceKey),
                DdcKeys.registryRevision(serviceKey),
                DdcKeys.registryCatalog(
                        serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                        serviceKey.serviceKind(), serviceKey.protocol()),
                DdcKeys.registryCatalogRevision(
                        serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                        serviceKey.serviceKind(), serviceKey.protocol()),
                DdcKeys.registryTopic(
                        serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                        serviceKey.serviceKind(), serviceKey.protocol()),
                DdcKeys.registryInstance(serviceKey, "scope") + ":lock"
        );

        assertThat(keys).allMatch(key -> key.startsWith("ddc:v3:{"));
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
