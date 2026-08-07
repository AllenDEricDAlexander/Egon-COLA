package top.egon.cola.component.ddc.admin.service.publish;

import org.springframework.stereotype.Component;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigResourceKey;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PublishResourceLockRegistry {

    private final ConcurrentMap<DdcConfigResourceKey, String> owners =
            new ConcurrentHashMap<>();

    public boolean tryAcquire(DdcConfigResourceKey key, String changeId) {
        if (key == null || changeId == null || changeId.isBlank()) {
            throw new IllegalArgumentException("resource key and changeId are required");
        }
        return owners.putIfAbsent(key, changeId) == null;
    }

    public void release(DdcConfigResourceKey key, String ownerChangeId) {
        if (key != null && ownerChangeId != null) {
            owners.remove(key, ownerChangeId);
        }
    }

    public Optional<String> owner(DdcConfigResourceKey key) {
        return Optional.ofNullable(owners.get(key));
    }
}
