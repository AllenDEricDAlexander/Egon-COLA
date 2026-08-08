package top.egon.cola.component.ddc.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin 通过消息通道发送给配置客户端的发布消息。
 * / Publication message sent by Admin to configuration clients through the message channel.
 */
public class DdcPublishMessage {

    /**
     * 配置变更标识。 / Configuration change identifier.
     */
    private String changeId;

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
     * 配置值的文本表示。 / Text representation of the configuration value.
     */
    private String configValue;

    /**
     * 配置值类型的线协议名称。 / Wire name of the configuration value type.
     */
    private String valueType;

    /**
     * 本次发布的目标版本。 / Target version of this publication.
     */
    private Long targetVersion;

    /**
     * 发布模式的线协议名称。 / Wire name of the publication mode.
     */
    private String publishMode;

    /**
     * 发布操作人。 / Publication operator.
     */
    private String operator;

    /**
     * 发布时间的毫秒时间戳。 / Publication time as an epoch-millisecond timestamp.
     */
    private Long timestamp;

    /**
     * 配置内容校验和。 / Configuration content checksum.
     */
    private String contentChecksum;

    /**
     * 本次发布的目标实例列表。 / Target instances of this publication.
     */
    private List<DdcPublishTarget> targets = new ArrayList<>();

    /**
     * 整条发布消息的校验和。 / Checksum of the complete publication message.
     */
    private String checksum;


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
     * 返回配置值文本。 / Returns the configuration value text.
     *
     * @return 配置值文本 / configuration value text
     */
    public String getConfigValue() {
        return configValue;
    }


    /**
     * 设置配置值文本。 / Sets the configuration value text.
     *
     * @param configValue 配置值文本 / configuration value text
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }


    /**
     * 返回配置值类型名称。 / Returns the configuration value type name.
     *
     * @return 值类型名称 / value type name
     */
    public String getValueType() {
        return valueType;
    }


    /**
     * 设置配置值类型名称。 / Sets the configuration value type name.
     *
     * @param valueType 值类型名称 / value type name
     */
    public void setValueType(String valueType) {
        this.valueType = valueType;
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
     * 返回发布模式名称。 / Returns the publication mode name.
     *
     * @return 发布模式名称 / publication mode name
     */
    public String getPublishMode() {
        return publishMode;
    }


    /**
     * 设置发布模式名称。 / Sets the publication mode name.
     *
     * @param publishMode 发布模式名称 / publication mode name
     */
    public void setPublishMode(String publishMode) {
        this.publishMode = publishMode;
    }


    /**
     * 返回发布操作人。 / Returns the publication operator.
     *
     * @return 发布操作人 / publication operator
     */
    public String getOperator() {
        return operator;
    }


    /**
     * 设置发布操作人。 / Sets the publication operator.
     *
     * @param operator 发布操作人 / publication operator
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }


    /**
     * 返回发布时间戳。 / Returns the publication timestamp.
     *
     * @return 毫秒时间戳 / epoch-millisecond timestamp
     */
    public Long getTimestamp() {
        return timestamp;
    }


    /**
     * 设置发布时间戳。 / Sets the publication timestamp.
     *
     * @param timestamp 毫秒时间戳 / epoch-millisecond timestamp
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
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
     * 返回目标实例的不可变副本。 / Returns an immutable copy of the target instances.
     *
     * @return 不可变目标列表 / immutable target list
     */
    public List<DdcPublishTarget> getTargets() {
        return List.copyOf(targets);
    }

    /**
     * 设置目标实例并保存可变防御副本。 / Sets target instances and stores a mutable defensive copy.
     *
     * @param targets 目标列表，空值按空列表处理 / target list, with null treated as an empty list
     */
    public void setTargets(List<DdcPublishTarget> targets) {
        this.targets = targets == null ? new ArrayList<>() : new ArrayList<>(targets);
    }


    /**
     * 返回消息校验和。 / Returns the message checksum.
     *
     * @return 消息校验和 / message checksum
     */
    public String getChecksum() {
        return checksum;
    }


    /**
     * 设置消息校验和。 / Sets the message checksum.
     *
     * @param checksum 消息校验和 / message checksum
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
