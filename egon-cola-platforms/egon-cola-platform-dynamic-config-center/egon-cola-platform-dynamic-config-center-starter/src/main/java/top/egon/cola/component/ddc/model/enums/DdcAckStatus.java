package top.egon.cola.component.ddc.model.enums;

/**
 * 配置变更确认状态。
 * / Configuration change acknowledgement status.
 */
public enum DdcAckStatus {
    /**
     * 确认应用成功。 / The change was applied successfully.
     */
    SUCCESS,

    /**
     * 确认应用失败。 / Applying the change failed.
     */
    FAILED,

    /**
     * 目标实例忽略了本次变更。 / The target instance ignored the change.
     */
    IGNORED,

    /**
     * 在约定时间内未收到确认。 / No acknowledgement was received in time.
     */
    TIMEOUT
}
