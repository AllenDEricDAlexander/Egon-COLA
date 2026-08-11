package top.egon.cola.component.ddc.model.management;

/**
 * Resource Server 准入撤销的幂等执行结果。
 * / Idempotent execution result of Resource Server admission revocation.
 *
 * @param configLeaseCount 已撤销的配置客户端租约数 / revoked configuration-client lease count
 * @param providerLeaseCount 已撤销的服务 Provider 租约数 / revoked service-provider lease count
 * @param persistedInstanceCount 已标记离线的持久化实例数 / persisted instances marked offline
 */
public record DdcResourceAdmissionRevocationResult(
        int configLeaseCount,
        int providerLeaseCount,
        int persistedInstanceCount
) {

    /**
     * 拒绝负数统计值。
     * / Rejects negative result counts.
     */
    public DdcResourceAdmissionRevocationResult {
        if (configLeaseCount < 0
                || providerLeaseCount < 0
                || persistedInstanceCount < 0) {
            throw new IllegalArgumentException(
                    "revocation counts must not be negative"
            );
        }
    }
}
