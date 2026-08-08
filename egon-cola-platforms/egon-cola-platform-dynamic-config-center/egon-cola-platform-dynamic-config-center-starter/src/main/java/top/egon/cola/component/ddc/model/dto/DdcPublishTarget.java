package top.egon.cola.component.ddc.model.dto;

/**
 * 配置发布的目标实例及其租约。
 * / Target instance and lease for a configuration publication.
 *
 * @param instanceId 目标实例标识 / target instance identifier
 * @param leaseId 目标实例的租约标识 / lease identifier of the target instance
 */
public record DdcPublishTarget(
        String instanceId,
        String leaseId
) {
}
