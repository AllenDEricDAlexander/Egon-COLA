package top.egon.cola.component.ddc.model.dto;

import top.egon.cola.component.ddc.model.enums.DdcAckStatus;

public class DdcAckRequest {

    private String changeId;

    private String instanceId;

    private String appCode;

    private String env;

    private String namespace;

    private String configKey;

    private Long targetVersion;

    private Long currentVersion;

    private DdcAckStatus status;

    private String errorMessage;

    private Long ackTime;

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public Long getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(Long targetVersion) {
        this.targetVersion = targetVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    public DdcAckStatus getStatus() {
        return status;
    }

    public void setStatus(DdcAckStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getAckTime() {
        return ackTime;
    }

    public void setAckTime(Long ackTime) {
        this.ackTime = ackTime;
    }
}
