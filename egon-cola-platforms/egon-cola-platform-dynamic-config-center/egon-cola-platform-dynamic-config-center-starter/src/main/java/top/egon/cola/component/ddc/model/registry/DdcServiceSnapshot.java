package top.egon.cola.component.ddc.model.registry;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * 指定服务键下的实例快照。
 * / Snapshot of instances registered under a service key.
 *
 * @param serviceKey 服务键 / service key
 * @param revision   服务实例集合修订号 / service instance set revision
 * @param instances  按实例标识和租约标识排序的实例列表 / instances sorted by instance and lease identifiers
 * @param observedAt 快照观测时间 / snapshot observation time
 */
public record DdcServiceSnapshot(
        DdcServiceKey serviceKey,
        long revision,
        @Nullable List<DdcServiceInstance> instances,
        @Nullable Instant observedAt
) {

    /**
     * 校验服务键，并规范化实例顺序与观测时间。
     * / Validates the service key and normalizes instance ordering and observation time.
     *
     * @throws IllegalArgumentException 服务键为空时抛出 / if the service key is {@code null}
     */
    public DdcServiceSnapshot {
        if (serviceKey == null) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        instances = instances == null
                ? List.of()
                : instances.stream().sorted().toList();
        observedAt = observedAt == null ? Instant.now() : observedAt;
    }
}
