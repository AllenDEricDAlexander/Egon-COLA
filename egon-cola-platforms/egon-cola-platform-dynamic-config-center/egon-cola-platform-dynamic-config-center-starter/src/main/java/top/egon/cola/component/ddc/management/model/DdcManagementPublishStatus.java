package top.egon.cola.component.ddc.management.model;

/**
 * 管理接口使用的聚合发布生命周期状态。 / Aggregate publication lifecycle status used by the management API.
 */
public enum DdcManagementPublishStatus {
    /**
     * 等待开始发布。 / Waiting for publication to begin.
     */
    PENDING,
    /**
     * 正在向目标实例发布。 / Publication to target instances is in progress.
     */
    PUBLISHING,
    /**
     * 所有必需目标均发布成功。 / All required targets completed successfully.
     */
    SUCCESS,
    /**
     * 仅部分目标发布成功。 / Only a subset of targets completed successfully.
     */
    PARTIAL_SUCCESS,
    /**
     * 发布失败。 / Publication failed.
     */
    FAILED,
    /**
     * 发布在完成前超时。 / Publication timed out before completion.
     */
    TIMEOUT,
    /**
     * 服务端返回了客户端无法识别的状态。 / The server returned a status unknown to this client.
     */
    UNKNOWN
}
