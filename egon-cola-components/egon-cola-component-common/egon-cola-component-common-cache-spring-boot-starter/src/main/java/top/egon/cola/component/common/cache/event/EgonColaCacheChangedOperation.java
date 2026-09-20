package top.egon.cola.component.common.cache.event;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * 全模块唯一缓存变更事件信封的封闭操作集（REQ-004 冻结字段语义）。稳定整数码显式声明，
 * 与 {@link Enum#ordinal()} 解耦：新增常量不会改写既有码值；JSON 载荷仍按常量名线格式传输。
 */
public enum EgonColaCacheChangedOperation implements EgonEnum {

    /** 回源写入后通告：对端仅逐出本地 L1。 */
    PUT(0, "PUT"),

    /** 精确键失效（双级）。 */
    EVICT(1, "EVICT"),

    /** 租户段 glob 失效（双级），键形状仅 {@code tenantId:*}。 */
    PREFIX_EVICT(2, "PREFIX_EVICT");

    private final int code;

    private final String message;

    EgonColaCacheChangedOperation(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
