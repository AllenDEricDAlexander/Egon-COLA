package top.egon.cola.component.ddc.admin.security.registration;

import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;

import java.io.Serial;

/**
 * Resource Server registration or heartbeat authentication failed.
 *
 * <p>Raised when Resource Server registration or heartbeat authentication fails.</p>
 */
public final class DdcRegistrationAuthenticationException extends DdcAdminException {

    /** Java 序列化版本；Java serialization version. */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 使用稳定 DDC 错误状态创建注册认证异常。
     *
     * <p>Creates a registration-authentication exception with a stable DDC error status.</p>
     *
     * @param status 注册认证错误状态；registration-authentication error status
     */
    public DdcRegistrationAuthenticationException(DdcErrorStatus status) {
        super(status);
    }

    /**
     * 使用稳定状态和内部原因创建注册认证异常。
     *
     * <p>Creates a registration-authentication exception with a stable status and internal cause.</p>
     *
     * @param status 注册认证错误状态；registration-authentication error status
     * @param cause 内部原因，禁止向调用方暴露原始 Token；internal cause that must not expose the
     * raw Token to callers
     */
    public DdcRegistrationAuthenticationException(
            DdcErrorStatus status,
            Throwable cause) {
        super(status, cause);
    }
}
