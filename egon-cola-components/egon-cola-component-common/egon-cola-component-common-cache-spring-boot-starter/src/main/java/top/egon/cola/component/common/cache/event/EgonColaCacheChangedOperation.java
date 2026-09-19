package top.egon.cola.component.common.cache.event;

/**
 * 缓存变更事件的封闭操作集（主 Spec REQ-004 冻结）。
 */
public enum EgonColaCacheChangedOperation {

    /** 回源写入后通告：对端仅逐出本地 L1。 */
    PUT,

    /** 精确键失效（双级）。 */
    EVICT,

    /** 租户段 glob 失效（双级），键形状仅 {@code tenantId:*}。 */
    PREFIX_EVICT
}
