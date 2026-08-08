package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

/**
 * 生成当前 v3 DDC 配置、租约、发布和服务注册使用的 Redis 键。 Generates current v3 Redis keys used by DDC configuration, leases, publications, and service registry.
 */
public final class DdcKeys {

    /**
     * 所有 DDC Redis 键的固定前缀。 Fixed prefix for all DDC Redis keys.
     */
    private static final String PREFIX = "ddc";

    /**
     * 禁止实例化键生成工具类。 Prevents instantiation of the key-generation utility.
     */
    private DdcKeys() {
    }

    /**
     * 生成以业务、环境和应用为作用域的 v3 配置值键。 Generates a v3 configuration-value key scoped by business, environment, and application.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param key     配置键。 configuration key
     * @return v3 配置 Redis 键。 v3 configuration Redis key
     */
    public static String v3Config(String bizCode, String env, String appCode, String key) {
        return v3(configV3Tag(bizCode, env, appCode), "config", key);
    }

    /**
     * 生成 v3 配置版本键。 Generates a v3 configuration-version key.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param key     配置键。 configuration key
     * @return v3 版本 Redis 键。 v3 version Redis key
     */
    public static String v3Version(String bizCode, String env, String appCode, String key) {
        return v3(configV3Tag(bizCode, env, appCode), "version", key);
    }

    /**
     * 生成 v3 配置变更 Topic。 Generates a v3 configuration-change topic.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return v3 配置变更 Topic 名称。 v3 configuration-change topic name
     */
    public static String v3Topic(String bizCode, String env, String appCode) {
        return v3(configV3Tag(bizCode, env, appCode), "topic");
    }

    /**
     * 生成 v3 发布幂等记录键。 Generates a v3 publication-idempotency key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param changeId 变更标识。 change identifier
     * @return v3 发布幂等 Redis 键。 v3 publication-idempotency Redis key
     */
    public static String v3PublishIdempotency(
            String bizCode,
            String env,
            String appCode,
            String changeId) {
        return v3(configV3Tag(bizCode, env, appCode), "publish", "idempotency", changeId);
    }

    /**
     * 生成 v3 单实例配置租约键。 Generates a v3 per-instance configuration-lease key.
     *
     * @param bizCode    业务编码。 business code
     * @param env        环境编码。 environment code
     * @param appCode    应用编码。 application code
     * @param instanceId 实例标识。 instance identifier
     * @return v3 配置租约 Redis 键。 v3 configuration-lease Redis key
     */
    public static String v3ConfigLeaseInstance(
            String bizCode,
            String env,
            String appCode,
            String instanceId) {
        return v3(configV3Tag(bizCode, env, appCode), "lease", "instance", instanceId);
    }

    /**
     * 生成 v3 配置租约实例集合键。 Generates a v3 configuration-lease instance collection key.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return v3 租约实例集合 Redis 键。 v3 lease-instance collection Redis key
     */
    public static String v3ConfigLeaseInstances(String bizCode, String env, String appCode) {
        return v3(configV3Tag(bizCode, env, appCode), "lease", "instances");
    }

    /**
     * 生成以 v3 服务作用域 hash tag 定位的注册实例键。 Generates a registry-instance key located by the v3 service-scope hash tag.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @param instanceId 实例标识。 instance identifier
     * @return v3 注册实例 Redis 键。 v3 registry-instance Redis key
     */
    public static String v3RegistryInstance(DdcServiceKey serviceKey, String instanceId) {
        return v3(registryV3Tag(serviceKey), "registry", "instance", instanceId);
    }

    /**
     * 生成以稳定服务标识结尾的 v3 服务定义键。 Generates a v3 service-definition key ending in the stable service identifier.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @return v3 服务定义 Redis 键。 v3 service-definition Redis key
     */
    public static String v3RegistryService(DdcServiceKey serviceKey) {
        return v3(registryV3Tag(serviceKey), "registry", "service", serviceKey.serviceId());
    }

    /**
     * 生成以稳定服务标识结尾的 v3 服务修订键。 Generates a v3 service-revision key ending in the stable service identifier.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @return v3 服务修订 Redis 键。 v3 service-revision Redis key
     */
    public static String v3RegistryRevision(DdcServiceKey serviceKey) {
        return v3(registryV3Tag(serviceKey), "registry", "revision", serviceKey.serviceId());
    }

    /**
     * 生成 v3 协议服务目录键。 Generates a v3 protocol-specific service-catalog key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param kind     服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return v3 服务目录 Redis 键。 v3 service-catalog Redis key
     */
    public static String v3RegistryCatalog(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return v3(registryV3Tag(bizCode, env, appCode, kind), "registry", "catalog", protocol);
    }

    /**
     * 生成 v3 协议服务目录修订键。 Generates a v3 protocol-specific service-catalog revision key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param kind     服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return v3 目录修订 Redis 键。 v3 catalog-revision Redis key
     */
    public static String v3RegistryCatalogRevision(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return v3(
                registryV3Tag(bizCode, env, appCode, kind),
                "registry", "catalog-revision", protocol
        );
    }

    /**
     * 生成 v3 协议服务注册变更 Topic。 Generates a v3 protocol-specific registry-change topic.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param kind     服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return v3 注册变更 Topic 名称。 v3 registry-change topic name
     */
    public static String v3RegistryTopic(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return v3(registryV3Tag(bizCode, env, appCode, kind), "registry", "topic", protocol);
    }

    /**
     * 生成跨作用域的 v3 全局服务目录键。 Generates the cross-scope v3 global service-catalog key.
     *
     * @return 固定的全局服务目录 Redis 键。 fixed global service-catalog Redis key
     */
    public static String v3GlobalRegistryCatalog() {
        return PREFIX + ":v3:{registry-catalog}:services";
    }

    /**
     * 生成跨作用域的 v3 全局服务目录修订键。 Generates the cross-scope v3 global service-catalog revision key.
     *
     * @return 固定的全局目录修订 Redis 键。 fixed global catalog-revision Redis key
     */
    public static String v3GlobalRegistryCatalogRevision() {
        return PREFIX + ":v3:{registry-catalog}:revision";
    }

    /**
     * 计算 v3 配置作用域的 Redis hash tag。 Computes the Redis hash tag for a v3 configuration scope.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String configV3Tag(String bizCode, String env, String appCode) {
        return tag(bizCode + "\n" + env + "\n" + appCode);
    }

    /**
     * 从规范服务键计算 v3 服务作用域 hash tag。 Computes a v3 service-scope hash tag from a canonical service key.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String registryV3Tag(DdcServiceKey serviceKey) {
        return registryV3Tag(
                serviceKey.bizCode(),
                serviceKey.env(),
                serviceKey.appCode(),
                serviceKey.serviceKind()
        );
    }

    /**
     * 计算 v3 服务作用域的 Redis hash tag。 Computes the Redis hash tag for a v3 service scope.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param kind    服务类型。 service kind
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String registryV3Tag(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind) {
        return tag(bizCode + "\n" + env + "\n" + appCode + "\n" + kind.name());
    }

    /**
     * 将作用域文本摘要包装为 Redis Cluster hash tag。 Wraps a scope-text digest as a Redis Cluster hash tag.
     *
     * @param value 待摘要的规范作用域文本。 canonical scope text to digest
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String tag(String value) {
        return "{" + digest(value) + "}";
    }

    /**
     * 计算文本的 SHA-256 摘要。 Computes the SHA-256 digest of text.
     *
     * @param value 待摘要文本。 text to digest
     * @return 十六进制摘要。 hexadecimal digest
     */
    private static String digest(String value) {
        return Digests.sha256Hex(value);
    }

    /**
     * 组合带版本和作用域 tag 的 v3 Redis 键。 Joins a versioned v3 Redis key with its scope tag.
     *
     * @param scopeTag Redis Cluster 作用域 tag。 Redis Cluster scope tag
     * @param parts    剩余键段。 remaining key segments
     * @return 完整 v3 Redis 键。 complete v3 Redis key
     */
    private static String v3(String scopeTag, String... parts) {
        return PREFIX + ":v3:" + scopeTag + ":" + String.join(":", parts);
    }
}
