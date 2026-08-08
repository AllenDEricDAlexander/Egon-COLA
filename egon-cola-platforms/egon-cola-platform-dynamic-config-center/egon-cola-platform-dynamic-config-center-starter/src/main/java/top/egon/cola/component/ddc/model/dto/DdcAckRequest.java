package top.egon.cola.component.ddc.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;

/**
 * 配置客户端向 Admin 上报的配置变更确认请求。
 * / Configuration change acknowledgement reported by a configuration client to Admin.
 */
public class DdcAckRequest {

    /**
     * 配置变更标识。 / Configuration change identifier.
     */
    private String changeId;

    /**
     * 确认实例标识。 / Acknowledging instance identifier.
     */
    private String instanceId;

    /**
     * 实例当前租约标识。 / Current lease identifier of the instance.
     */
    private String leaseId;

    /**
     * 业务编码。 / Business code.
     */
    private String bizCode;

    /**
     * 应用编码。 / Application code.
     */
    private String appCode;

    /**
     * 运行环境。 / Runtime environment.
     */
    private String env;

    /**
     * 已废弃的 namespace 兼容字段。 / Deprecated namespace compatibility field.
     */
    private String namespace;

    /**
     * 配置键。 / Configuration key.
     */
    private String configKey;

    /**
     * 本次发布的目标版本。 / Target version of this publication.
     */
    private Long targetVersion;

    /**
     * 实例确认时持有的当前版本。 / Current version held by the instance when acknowledging.
     */
    private Long currentVersion;

    /**
     * 已应用配置内容的校验和。 / Checksum of the applied configuration content.
     */
    private String contentChecksum;

    /**
     * 确认状态。 / Acknowledgement status.
     */
    private DdcAckStatus status;

    /**
     * 失败或忽略原因。 / Failure or ignore reason.
     */
    private String errorMessage;

    /**
     * 确认时间的毫秒时间戳。 / Acknowledgement time as an epoch-millisecond timestamp.
     */
    private Long ackTime;

    /**
     * 返回配置变更标识。 / Returns the configuration change identifier.
     *
     * @return 配置变更标识 / configuration change identifier
     */
    public String getChangeId() {
        return changeId;
    }

    /**
     * 设置配置变更标识。 / Sets the configuration change identifier.
     *
     * @param changeId 配置变更标识 / configuration change identifier
     */
    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    /**
     * 返回确认实例标识。 / Returns the acknowledging instance identifier.
     *
     * @return 实例标识 / instance identifier
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 设置确认实例标识。 / Sets the acknowledging instance identifier.
     *
     * @param instanceId 实例标识 / instance identifier
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * 返回租约标识。 / Returns the lease identifier.
     *
     * @return 租约标识 / lease identifier
     */
    public String getLeaseId() {
        return leaseId;
    }

    /**
     * 设置租约标识。 / Sets the lease identifier.
     *
     * @param leaseId 租约标识 / lease identifier
     */
    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
    }

    /**
     * 返回业务编码。 / Returns the business code.
     *
     * @return 业务编码 / business code
     */
    public String getBizCode() {
        return bizCode;
    }

    /**
     * 设置业务编码。 / Sets the business code.
     *
     * @param bizCode 业务编码 / business code
     */
    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    /**
     * 返回应用编码。 / Returns the application code.
     *
     * @return 应用编码 / application code
     */
    public String getAppCode() {
        return appCode;
    }

    /**
     * 设置应用编码。 / Sets the application code.
     *
     * @param appCode 应用编码 / application code
     */
    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    /**
     * 返回运行环境。 / Returns the runtime environment.
     *
     * @return 运行环境 / runtime environment
     */
    public String getEnv() {
        return env;
    }

    /**
     * 设置运行环境。 / Sets the runtime environment.
     *
     * @param env 运行环境 / runtime environment
     */
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * 返回已废弃的 namespace 兼容值。 / Returns the deprecated namespace compatibility value.
     *
     * @return namespace 兼容值 / namespace compatibility value
     * @deprecated namespace 不再参与物理配置范围。 / namespace no longer participates in the physical configuration scope.
     */
    @JsonIgnore
    @Deprecated(forRemoval = true)
    public String getNamespace() {
        return namespace;
    }

    /**
     * 设置已废弃的 namespace 兼容值。 / Sets the deprecated namespace compatibility value.
     *
     * @param namespace namespace 兼容值 / namespace compatibility value
     * @deprecated namespace 不再参与物理配置范围。 / namespace no longer participates in the physical configuration scope.
     */
    @JsonIgnore
    @Deprecated(forRemoval = true)
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 返回配置键。 / Returns the configuration key.
     *
     * @return 配置键 / configuration key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 设置配置键。 / Sets the configuration key.
     *
     * @param configKey 配置键 / configuration key
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    /**
     * 返回目标版本。 / Returns the target version.
     *
     * @return 目标版本 / target version
     */
    public Long getTargetVersion() {
        return targetVersion;
    }

    /**
     * 设置目标版本。 / Sets the target version.
     *
     * @param targetVersion 目标版本 / target version
     */
    public void setTargetVersion(Long targetVersion) {
        this.targetVersion = targetVersion;
    }

    /**
     * 返回实例当前版本。 / Returns the instance's current version.
     *
     * @return 当前版本 / current version
     */
    public Long getCurrentVersion() {
        return currentVersion;
    }

    /**
     * 设置实例当前版本。 / Sets the instance's current version.
     *
     * @param currentVersion 当前版本 / current version
     */
    public void setCurrentVersion(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    /**
     * 返回内容校验和。 / Returns the content checksum.
     *
     * @return 内容校验和 / content checksum
     */
    public String getContentChecksum() {
        return contentChecksum;
    }

    /**
     * 设置内容校验和。 / Sets the content checksum.
     *
     * @param contentChecksum 内容校验和 / content checksum
     */
    public void setContentChecksum(String contentChecksum) {
        this.contentChecksum = contentChecksum;
    }

    /**
     * 返回确认状态。 / Returns the acknowledgement status.
     *
     * @return 确认状态 / acknowledgement status
     */
    public DdcAckStatus getStatus() {
        return status;
    }

    /**
     * 设置确认状态。 / Sets the acknowledgement status.
     *
     * @param status 确认状态 / acknowledgement status
     */
    public void setStatus(DdcAckStatus status) {
        this.status = status;
    }

    /**
     * 返回失败或忽略原因。 / Returns the failure or ignore reason.
     *
     * @return 错误消息 / error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置失败或忽略原因。 / Sets the failure or ignore reason.
     *
     * @param errorMessage 错误消息 / error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 返回确认时间的毫秒时间戳。 / Returns the acknowledgement time as epoch milliseconds.
     *
     * @return 确认时间戳 / acknowledgement timestamp
     */
    public Long getAckTime() {
        return ackTime;
    }

    /**
     * 设置确认时间的毫秒时间戳。 / Sets the acknowledgement time as epoch milliseconds.
     *
     * @param ackTime 确认时间戳 / acknowledgement timestamp
     */
    public void setAckTime(Long ackTime) {
        this.ackTime = ackTime;
    }
}
