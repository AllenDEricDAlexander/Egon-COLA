package top.egon.cola.component.tianshu.admin.service.publish;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import top.egon.cola.component.tianshu.admin.model.vo.DdcConfigResourceKey;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component("publishResourceLockRegistry")
public class PublishResourceLockRegistry {

    private final ConcurrentMap<TenantResourceKey, String> owners =
            new ConcurrentHashMap<>();

    public boolean tryAcquire(DdcConfigResourceKey key, String changeId) {
        if (key == null || changeId == null || changeId.isBlank()) {
            throw new IllegalArgumentException("resource key and changeId are required");
        }
        TenantResourceKey tenantResourceKey = new TenantResourceKey(tenantId(), key);
        boolean acquired = owners.putIfAbsent(tenantResourceKey, changeId) == null;
        if (!acquired) {
            log.debug("Tianshu publish resource lock is already owned for tenantId={}",
                    tenantResourceKey.tenantId());
        }
        return acquired;
    }

    public void release(DdcConfigResourceKey key, String ownerChangeId) {
        if (key != null && ownerChangeId != null) {
            owners.remove(new TenantResourceKey(tenantId(), key), ownerChangeId);
        }
    }

    public Optional<String> owner(DdcConfigResourceKey key) {
        return Optional.ofNullable(owners.get(new TenantResourceKey(tenantId(), key)));
    }

    private long tenantId() {
        String value = MDC.get("tenantId");
        try {
            long tenantId = Long.parseLong(value);
            if (tenantId > 0 && Long.toString(tenantId).equals(value)) {
                return tenantId;
            }
        } catch (NumberFormatException ignored) {
            // A request without trusted tenant MDC is rejected below.
        }
        throw new IllegalStateException("A positive trusted tenantId is required for publish resource locking");
    }

    private record TenantResourceKey(long tenantId, DdcConfigResourceKey resourceKey) {
    }
}
