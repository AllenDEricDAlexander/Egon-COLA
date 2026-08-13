package top.egon.cola.component.rpc.provider.registration;

import java.time.Instant;

/**
 * RPC Provider 租约操作的注册中心无关结果。
 *
 * <p>Registry-neutral outcome of an RPC Provider lease operation.
 */
public record RpcLeaseOperationResult(
        Status status,
        Instant leaseExpireAt
) {

    public RpcLeaseOperationResult {
        if (status == null) {
            throw new IllegalArgumentException(
                    "RPC lease operation status is required"
            );
        }
    }

    /**
     * 租约操作状态。
     *
     * <p>Lease operation status.
     */
    public enum Status {
        RENEWED,
        DELETED,
        NOT_FOUND,
        LEASE_MISMATCH
    }

    /**
     * 判断本次操作是否成功续约。
     *
     * <p>Returns whether this operation renewed the lease.
     *
     * @return 是否已续约 / whether the lease was renewed
     */
    public boolean renewed() {
        return status == Status.RENEWED;
    }

    /**
     * 创建续约成功结果。
     *
     * <p>Creates a successful renewal result.
     *
     * @param leaseExpireAt 新的过期时间 / new expiration time
     * @return 续约成功结果 / successful renewal result
     */
    public static RpcLeaseOperationResult renewed(Instant leaseExpireAt) {
        return new RpcLeaseOperationResult(Status.RENEWED, leaseExpireAt);
    }

    /**
     * 创建删除成功结果。
     *
     * <p>Creates a successful deletion result.
     *
     * @return 删除结果 / deletion result
     */
    public static RpcLeaseOperationResult deleted() {
        return new RpcLeaseOperationResult(Status.DELETED, null);
    }

    /**
     * 创建租约不存在结果。
     *
     * <p>Creates a lease-not-found result.
     *
     * @return 不存在结果 / not-found result
     */
    public static RpcLeaseOperationResult notFound() {
        return new RpcLeaseOperationResult(Status.NOT_FOUND, null);
    }

    /**
     * 创建租约不匹配结果。
     *
     * <p>Creates a lease-mismatch result.
     *
     * @return 不匹配结果 / mismatch result
     */
    public static RpcLeaseOperationResult leaseMismatch() {
        return new RpcLeaseOperationResult(Status.LEASE_MISMATCH, null);
    }
}
