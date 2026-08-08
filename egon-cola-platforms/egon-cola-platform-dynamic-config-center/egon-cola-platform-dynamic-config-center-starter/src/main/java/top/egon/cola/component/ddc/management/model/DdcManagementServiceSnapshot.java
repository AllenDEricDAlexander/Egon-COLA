package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.List;

/**
 * 某个服务键在特定代次的实例快照。 / Instance snapshot of a service key at a specific generation.
 *
 * @param serviceKey 快照对应的服务键 / service key represented by the snapshot
 * @param generation 服务注册表代次 / service-registry generation
 * @param observedAt 快照观测时间 / snapshot observation time
 * @param instances 快照中的服务实例 / service instances in the snapshot
 */
public record DdcManagementServiceSnapshot(
        DdcManagementServiceKey serviceKey,
        long generation,
        Instant observedAt,
        List<DdcManagementServiceInstance> instances
) {

    /**
     * 构造服务快照并将实例列表归一化为不可变列表。 /
     * Constructs a service snapshot and normalizes the instance list to an immutable list.
     */
    public DdcManagementServiceSnapshot {
        instances = instances == null ? List.of() : List.copyOf(instances);
    }
}
