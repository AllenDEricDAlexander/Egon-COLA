package top.egon.cola.component.ddc.model.registry;

import java.time.Instant;
import java.util.List;

/**
 * 服务目录查询结果快照。
 * / Snapshot returned by a service catalog query.
 *
 * @param query 生成该快照的查询条件 / query that produced this snapshot
 * @param revision 服务目录修订号 / service catalog revision
 * @param serviceKeys 按规范顺序排列的服务键 / service keys in canonical order
 * @param observedAt 快照观测时间 / snapshot observation time
 */
public record DdcServiceCatalogSnapshot(
        DdcServiceQuery query,
        long revision,
        List<DdcServiceKey> serviceKeys,
        Instant observedAt
) {

    /**
     * 校验查询条件，并规范化服务键顺序与观测时间。
     * / Validates the query and normalizes service key ordering and observation time.
     *
     * @throws IllegalArgumentException 查询条件为空时抛出 / if the query is {@code null}
     */
    public DdcServiceCatalogSnapshot {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        serviceKeys = serviceKeys == null
                ? List.of()
                : serviceKeys.stream().sorted().toList();
        observedAt = observedAt == null ? Instant.now() : observedAt;
    }
}
