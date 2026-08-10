package top.egon.cola.platform.idp.core.resource;

/**
 * Resource Server 的管理状态。
 *
 * <p>Administrative status of a Resource Server.</p>
 */
public enum ResourceServerStatus {

    /**
     * Resource Server 可签发令牌和准入票据。
     *
     * <p>The Resource Server may receive tokens and admission tickets.</p>
     */
    ACTIVE,

    /**
     * Resource Server 已停止令牌签发和实例续租。
     *
     * <p>The Resource Server no longer receives tokens or lease renewals.</p>
     */
    DISABLED
}
