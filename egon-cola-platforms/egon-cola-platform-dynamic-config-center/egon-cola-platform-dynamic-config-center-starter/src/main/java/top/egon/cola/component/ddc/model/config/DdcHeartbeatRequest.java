package top.egon.cola.component.ddc.model.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * 配置客户端实例的租约心跳请求。
 * / Lease heartbeat request for a configuration client instance.
 */
public class DdcHeartbeatRequest {

    /**
     * 实例标识。 / Instance identifier.
     */
    private String instanceId;

    /**
     * 租约标识。 / Lease identifier.
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
     * 实例主机地址。 / Instance host address.
     */
    private String host;

    /**
     * 实例端口。 / Instance port.
     */
    private Integer port;

    /**
     * 进程标识。 / Process identifier.
     */
    private String pid;

    /**
     * 客户端 SDK 版本。 / Client SDK version.
     */
    private String sdkVersion;

    /**
     * 随心跳上报的不可变实例元数据。 / Immutable instance metadata reported with the heartbeat.
     */
    private Map<String, String> metadata = Map.of();


    /**
     * 返回实例标识。 / Returns the instance identifier.
     *
     * @return 实例标识 / instance identifier
     */
    public String getInstanceId() {
        return instanceId;
    }


    /**
     * 设置实例标识。 / Sets the instance identifier.
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
     * @deprecated namespace 不再参与物理实例范围。 / namespace no longer participates in the physical instance scope.
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
     * @deprecated namespace 不再参与物理实例范围。 / namespace no longer participates in the physical instance scope.
     */
    @JsonIgnore
    @Deprecated(forRemoval = true)
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }


    /**
     * 返回实例主机地址。 / Returns the instance host address.
     *
     * @return 主机地址 / host address
     */
    public String getHost() {
        return host;
    }


    /**
     * 设置实例主机地址。 / Sets the instance host address.
     *
     * @param host 主机地址 / host address
     */
    public void setHost(String host) {
        this.host = host;
    }


    /**
     * 返回实例端口。 / Returns the instance port.
     *
     * @return 实例端口 / instance port
     */
    public Integer getPort() {
        return port;
    }


    /**
     * 设置实例端口。 / Sets the instance port.
     *
     * @param port 实例端口 / instance port
     */
    public void setPort(Integer port) {
        this.port = port;
    }


    /**
     * 返回进程标识。 / Returns the process identifier.
     *
     * @return 进程标识 / process identifier
     */
    public String getPid() {
        return pid;
    }


    /**
     * 设置进程标识。 / Sets the process identifier.
     *
     * @param pid 进程标识 / process identifier
     */
    public void setPid(String pid) {
        this.pid = pid;
    }


    /**
     * 返回客户端 SDK 版本。 / Returns the client SDK version.
     *
     * @return SDK 版本 / SDK version
     */
    public String getSdkVersion() {
        return sdkVersion;
    }


    /**
     * 设置客户端 SDK 版本。 / Sets the client SDK version.
     *
     * @param sdkVersion SDK 版本 / SDK version
     */
    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }


    /**
     * 返回实例元数据。 / Returns the instance metadata.
     *
     * @return 不可变实例元数据 / immutable instance metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * 设置实例元数据并保存不可变副本。 / Sets instance metadata and stores an immutable copy.
     *
     * @param metadata 实例元数据，空值按空映射处理 / instance metadata, with null treated as an empty map
     * @throws NullPointerException 元数据包含空键或空值时抛出 / if metadata contains a null key or value
     */
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
