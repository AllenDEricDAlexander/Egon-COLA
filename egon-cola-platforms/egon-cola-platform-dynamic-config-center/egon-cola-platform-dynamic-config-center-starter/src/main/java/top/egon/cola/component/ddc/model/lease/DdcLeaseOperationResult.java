package top.egon.cola.component.ddc.model.lease;

import org.springframework.lang.Nullable;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;

import java.time.Instant;

/**
 * 租约续期或注销操作的结果。
 * / Result of a lease renewal or deregistration operation.
 *
 * @param status        操作状态 / operation status
 * @param leaseExpireAt 操作后的租约到期时间，可为空 / lease expiration time after the operation, nullable
 */
public record DdcLeaseOperationResult(
        DdcLeaseOperationStatus status,
        @Nullable Instant leaseExpireAt
) {

    /**
     * 判断租约是否已成功续期。
     * / Indicates whether the lease was renewed successfully.
     *
     * @return 已成功续期时为 {@code true} / {@code true} when renewed successfully
     */
    public boolean renewed() {
        return status == DdcLeaseOperationStatus.RENEWED;
    }

    /**
     * 判断租约是否已成功删除。
     * / Indicates whether the lease was deleted successfully.
     *
     * @return 已成功删除时为 {@code true} / {@code true} when deleted successfully
     */
    public boolean deleted() {
        return status == DdcLeaseOperationStatus.DELETED;
    }
}
