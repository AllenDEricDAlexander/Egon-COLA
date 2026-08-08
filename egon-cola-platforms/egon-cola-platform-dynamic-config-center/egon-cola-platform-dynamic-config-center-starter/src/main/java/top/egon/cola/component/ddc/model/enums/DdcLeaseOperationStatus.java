package top.egon.cola.component.ddc.model.enums;

/**
 * 租约操作结果状态。
 * / Lease operation result status.
 */
public enum DdcLeaseOperationStatus {

    /**
     * 租约已成功续期。 / The lease was renewed successfully.
     */
    RENEWED,

    /**
     * 租约已成功删除。 / The lease was deleted successfully.
     */
    DELETED,

    /**
     * 未找到目标租约。 / The target lease was not found.
     */
    NOT_FOUND,

    /**
     * 实例与租约标识不匹配。 / The instance and lease identifiers do not match.
     */
    LEASE_MISMATCH,

    /**
     * 租约未被删除。 / The lease was not deleted.
     */
    NOT_DELETED
}
