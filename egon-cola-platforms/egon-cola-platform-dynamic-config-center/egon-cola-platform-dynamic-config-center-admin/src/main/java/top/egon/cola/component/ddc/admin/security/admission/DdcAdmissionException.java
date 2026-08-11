package top.egon.cola.component.ddc.admin.security.admission;

import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;

import java.io.Serial;

/**
 * Resource Server 注册或心跳准入失败。
 *
 * <p>Raised when Resource Server registration or heartbeat admission fails.</p>
 */
public final class DdcAdmissionException extends DdcAdminException {

    /** Java 序列化版本；Java serialization version. */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 使用稳定 DDC 错误状态创建准入异常。
     *
     * <p>Creates an admission exception with a stable DDC error status.</p>
     *
     * @param status 准入错误状态；admission error status
     */
    public DdcAdmissionException(DdcErrorStatus status) {
        super(status);
    }

    /**
     * 使用稳定状态和内部原因创建准入异常。
     *
     * <p>Creates an admission exception with a stable status and internal cause.</p>
     *
     * @param status 准入错误状态；admission error status
     * @param cause 内部原因，禁止向调用方暴露原始票据；internal cause that must not expose the
     * raw ticket to callers
     */
    public DdcAdmissionException(DdcErrorStatus status, Throwable cause) {
        super(status, cause);
    }
}
