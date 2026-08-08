package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

/**
 * 生成 DDC 配置、租约、发布和服务注册使用的 Redis 键。
 * 当前键格式版本为 v3，所有键均以 {@code ddc:v3:} 开头。
 * Generates Redis keys used by DDC configuration, leases, publications, and service registry.
 * The current key format version is v3, and every key starts with {@code ddc:v3:}.
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
     * 生成以业务、环境和应用为作用域的配置值键。 Generates a configuration-value key scoped by business, environment, and application.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param key     配置键。 configuration key
     * @return 配置 Redis 键。 configuration Redis key
     */
    public static String config(String bizCode, String env, String appCode, String key) {
        return buildKey(configTag(bizCode, env, appCode), "config", key);
    }

    /**
     * 生成配置版本键。 Generates a configuration-version key.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param key     配置键。 configuration key
     * @return 版本 Redis 键。 version Redis key
     */
    public static String version(String bizCode, String env, String appCode, String key) {
        return buildKey(configTag(bizCode, env, appCode), "version", key);
    }

    /**
     * 生成配置变更 Topic。 Generates a configuration-change topic.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return 配置变更 Topic 名称。 configuration-change topic name
     */
    public static String topic(String bizCode, String env, String appCode) {
        return buildKey(configTag(bizCode, env, appCode), "topic");
    }

    /**
     * 生成发布幂等记录键。 Generates a publication-idempotency key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param changeId 变更标识。 change identifier
     * @return 发布幂等 Redis 键。 publication-idempotency Redis key
     */
    public static String publishIdempotency(
            String bizCode,
            String env,
            String appCode,
            String changeId) {
        return buildKey(configTag(bizCode, env, appCode), "publish", "idempotency", changeId);
    }

    /**
     * 生成单实例配置租约键。 Generates a per-instance configuration-lease key.
     *
     * @param bizCode    业务编码。 business code
     * @param env        环境编码。 environment code
     * @param appCode    应用编码。 application code
     * @param instanceId 实例标识。 instance identifier
     * @return 配置租约 Redis 键。 configuration-lease Redis key
     */
    public static String configLeaseInstance(
            String bizCode,
            String env,
            String appCode,
            String instanceId) {
        return buildKey(configTag(bizCode, env, appCode), "lease", "instance", instanceId);
    }

    /**
     * 生成配置租约实例集合键。 Generates a configuration-lease instance collection key.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return 租约实例集合 Redis 键。 lease-instance collection Redis key
     */
    public static String configLeaseInstances(String bizCode, String env, String appCode) {
        return buildKey(configTag(bizCode, env, appCode), "lease", "instances");
    }

    /**
     * 生成以服务作用域 hash tag 定位的注册实例键。 Generates a registry-instance key located by the service-scope hash tag.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @param instanceId 实例标识。 instance identifier
     * @return 注册实例 Redis 键。 registry-instance Redis key
     */
    public static String registryInstance(DdcServiceKey serviceKey, String instanceId) {
        return buildKey(registryTag(serviceKey), "registry", "instance", instanceId);
    }

    /**
     * 生成以稳定服务标识结尾的服务定义键。 Generates a service-definition key ending in the stable service identifier.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @return 服务定义 Redis 键。 service-definition Redis key
     */
    public static String registryService(DdcServiceKey serviceKey) {
        return buildKey(registryTag(serviceKey), "registry", "service", serviceKey.serviceId());
    }

    /**
     * 生成以稳定服务标识结尾的服务修订键。 Generates a service-revision key ending in the stable service identifier.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @return 服务修订 Redis 键。 service-revision Redis key
     */
    public static String registryRevision(DdcServiceKey serviceKey) {
        return buildKey(registryTag(serviceKey), "registry", "revision", serviceKey.serviceId());
    }

    /**
     * 生成协议服务目录键。 Generates a protocol-specific service-catalog key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param kind     服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return 服务目录 Redis 键。 service-catalog Redis key
     */
    public static String registryCatalog(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return buildKey(registryTag(bizCode, env, appCode, kind), "registry", "catalog", protocol);
    }

    /**
     * 生成协议服务目录修订键。 Generates a protocol-specific service-catalog revision key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param kind     服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return 目录修订 Redis 键。 catalog-revision Redis key
     */
    public static String registryCatalogRevision(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return buildKey(
                registryTag(bizCode, env, appCode, kind),
                "registry", "catalog-revision", protocol
        );
    }

    /**
     * 生成协议服务注册变更 Topic。 Generates a protocol-specific registry-change topic.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param kind     服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return 注册变更 Topic 名称。 registry-change topic name
     */
    public static String registryTopic(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return buildKey(registryTag(bizCode, env, appCode, kind), "registry", "topic", protocol);
    }

    /**
     * 生成跨作用域的全局服务目录键。 Generates the cross-scope global service-catalog key.
     *
     * @return 固定的全局服务目录 Redis 键。 fixed global service-catalog Redis key
     */
    public static String globalRegistryCatalog() {
        return PREFIX + ":v3:{registry-catalog}:services";
    }

    /**
     * 生成跨作用域的全局服务目录修订键。 Generates the cross-scope global service-catalog revision key.
     *
     * @return 固定的全局目录修订 Redis 键。 fixed global catalog-revision Redis key
     */
    public static String globalRegistryCatalogRevision() {
        return PREFIX + ":v3:{registry-catalog}:revision";
    }

    /**
     * 计算配置作用域的 Redis hash tag。 Computes the Redis hash tag for a configuration scope.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String configTag(String bizCode, String env, String appCode) {
        return tag(bizCode + "\n" + env + "\n" + appCode);
    }

    /**
     * 从规范服务键计算服务作用域 hash tag。 Computes a service-scope hash tag from a canonical service key.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String registryTag(DdcServiceKey serviceKey) {
        return registryTag(
                serviceKey.bizCode(),
                serviceKey.env(),
                serviceKey.appCode(),
                serviceKey.serviceKind()
        );
    }

    /**
     * 计算服务作用域的 Redis hash tag。 Computes the Redis hash tag for a service scope.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param kind    服务类型。 service kind
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String registryTag(
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
     * 组合带版本和作用域 tag 的 Redis 键。 Builds a versioned Redis key with its scope tag.
     *
     * @param scopeTag Redis Cluster 作用域 tag。 Redis Cluster scope tag
     * @param parts    剩余键段。 remaining key segments
     * @return 完整 Redis 键。 complete Redis key
     */
    private static String buildKey(String scopeTag, String... parts) {
        return PREFIX + ":v3:" + scopeTag + ":" + String.join(":", parts);
    }
}
