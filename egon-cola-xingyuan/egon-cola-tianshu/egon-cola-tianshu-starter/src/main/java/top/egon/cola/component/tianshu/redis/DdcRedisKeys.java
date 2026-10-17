package top.egon.cola.component.tianshu.redis;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;

/**
 * 生成 Tianshu 配置、租约、发布和服务注册使用的 Redis 键。
 * 当前键格式为 {@code tianshu:v4:tenant:<tenantId>:{<slotHash>}:<suffix>}。
 * Generates Redis keys used by Tianshu configuration, leases, publications, and service registry.
 * The v4 format isolates keys and hash slots by tenant.
 */
public final class DdcRedisKeys {

    /**
     * 所有 Tianshu Redis 键的固定前缀。 Fixed prefix for all Tianshu Redis keys.
     */
    private static final String PREFIX = "tianshu";

    /**
     * 当前 Tianshu Redis 键格式版本。 Current Tianshu Redis key format version.
     */
    private static final String KEY_VERSION = "v4";

    /**
     * 禁止实例化键生成工具类。 Prevents instantiation of the key-generation utility.
     */
    private DdcRedisKeys() {
    }

    /**
     * 生成以业务、环境和应用为作用域的配置值键。 Generates a configuration-value key scoped by business, environment, and application.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param key     配置键。 configuration key
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 配置 Redis 键。 configuration Redis key
     */
    public static String config(Long tenantId, String bizCode, String env, String appCode, String key) {
        return buildKey(tenantId, configTag(tenantId, bizCode, env, appCode), "config", key);
    }

    /**
     * 生成配置版本键。 Generates a configuration-version key.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param key     配置键。 configuration key
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 版本 Redis 键。 version Redis key
     */
    public static String version(Long tenantId, String bizCode, String env, String appCode, String key) {
        return buildKey(tenantId, configTag(tenantId, bizCode, env, appCode), "version", key);
    }

    /**
     * 生成配置变更 Topic。 Generates a configuration-change topic.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 配置变更 Topic 名称。 configuration-change topic name
     */
    public static String topic(Long tenantId, String bizCode, String env, String appCode) {
        return buildKey(tenantId, configTag(tenantId, bizCode, env, appCode), "topic");
    }

    /**
     * 生成 AI Prompt/Skill 发布事实 Topic，与配置 Topic 共享同一作用域 hash tag。
     * Generates the AI Prompt/Skill publication topic sharing the configuration scope hash tag.
     *
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @return AI 发布 Topic 名称。 AI publication topic name
     */
    public static String aiTopic(Long tenantId, String bizCode, String env, String appCode) {
        return buildKey(tenantId, configTag(tenantId, bizCode, env, appCode), "ai", "topic");
    }

    /**
     * 生成发布幂等记录键。 Generates a publication-idempotency key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param changeId 变更标识。 change identifier
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 发布幂等 Redis 键。 publication-idempotency Redis key
     */
    public static String publishIdempotency(
            Long tenantId,
            String bizCode,
            String env,
            String appCode,
            String changeId) {
        return buildKey(tenantId, configTag(tenantId, bizCode, env, appCode),
                "publish", "idempotency", changeId);
    }

    /**
     * 生成单实例配置租约键。 Generates a per-instance configuration-lease key.
     *
     * @param bizCode    业务编码。 business code
     * @param env        环境编码。 environment code
     * @param appCode    应用编码。 application code
     * @param instanceId 实例标识。 instance identifier
     * @param tenantId   存储租户标识。 storage tenant identifier
     * @return 配置租约 Redis 键。 configuration-lease Redis key
     */
    public static String configLeaseInstance(
            Long tenantId,
            String bizCode,
            String env,
            String appCode,
            String instanceId) {
        return buildKey(tenantId, configTag(tenantId, bizCode, env, appCode),
                "lease", "instance", instanceId);
    }

    /**
     * 生成配置租约实例集合键。 Generates a configuration-lease instance collection key.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 租约实例集合 Redis 键。 lease-instance collection Redis key
     */
    public static String configLeaseInstances(Long tenantId, String bizCode, String env, String appCode) {
        return buildKey(tenantId, configTag(tenantId, bizCode, env, appCode),
                "lease", "instances");
    }

    /**
     * 生成以服务作用域 hash tag 定位的注册实例键。 Generates a registry-instance key located by the service-scope hash tag.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @param instanceId 实例标识。 instance identifier
     * @param tenantId   存储租户标识。 storage tenant identifier
     * @return 注册实例 Redis 键。 registry-instance Redis key
     */
    public static String registryInstance(Long tenantId, DdcServiceKey serviceKey, String instanceId) {
        return buildKey(tenantId, registryTag(tenantId, serviceKey),
                "registry", "instance", instanceId);
    }

    /**
     * 生成以稳定服务标识结尾的服务定义键。 Generates a service-definition key ending in the stable service identifier.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @param tenantId   存储租户标识。 storage tenant identifier
     * @return 服务定义 Redis 键。 service-definition Redis key
     */
    public static String registryService(Long tenantId, DdcServiceKey serviceKey) {
        return buildKey(tenantId, registryTag(tenantId, serviceKey),
                "registry", "service", serviceKey.serviceId());
    }

    /**
     * 生成以稳定服务标识结尾的服务修订键。 Generates a service-revision key ending in the stable service identifier.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @param tenantId   存储租户标识。 storage tenant identifier
     * @return 服务修订 Redis 键。 service-revision Redis key
     */
    public static String registryRevision(Long tenantId, DdcServiceKey serviceKey) {
        return buildKey(tenantId, registryTag(tenantId, serviceKey),
                "registry", "revision", serviceKey.serviceId());
    }

    /**
     * 生成协议服务目录键。 Generates a protocol-specific service-catalog key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param kind     服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 服务目录 Redis 键。 service-catalog Redis key
     */
    public static String registryCatalog(
            Long tenantId,
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return buildKey(tenantId, registryTag(tenantId, bizCode, env, appCode, kind),
                "registry", "catalog", protocol);
    }

    /**
     * 生成协议服务目录修订键。 Generates a protocol-specific service-catalog revision key.
     *
     * @param bizCode  业务编码。 business code
     * @param env      环境编码。 environment code
     * @param appCode  应用编码。 application code
     * @param kind     服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 目录修订 Redis 键。 catalog-revision Redis key
     */
    public static String registryCatalogRevision(
            Long tenantId,
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return buildKey(
                tenantId,
                registryTag(tenantId, bizCode, env, appCode, kind),
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
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 注册变更 Topic 名称。 registry-change topic name
     */
    public static String registryTopic(
            Long tenantId,
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return buildKey(tenantId, registryTag(tenantId, bizCode, env, appCode, kind),
                "registry", "topic", protocol);
    }

    /**
     * 生成tenant内跨物理服务scope的全局目录键。 Generates the tenant-local global service-catalog key.
     *
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 当前tenant的全局服务目录Redis键。 tenant-local global service-catalog Redis key
     */
    public static String globalRegistryCatalog(Long tenantId) {
        return buildKey(tenantId, tag(tenantId(tenantId) + "\nregistry-catalog"), "services");
    }

    /**
     * 生成tenant内跨物理服务scope的目录修订键。 Generates the tenant-local global service-catalog revision key.
     *
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 当前tenant的全局目录修订Redis键。 tenant-local global catalog-revision Redis key
     */
    public static String globalRegistryCatalogRevision(Long tenantId) {
        return buildKey(tenantId, tag(tenantId(tenantId) + "\nregistry-catalog"), "revision");
    }

    /**
     * 计算配置作用域的 Redis hash tag。 Computes the Redis hash tag for a configuration scope.
     *
     * @param bizCode 业务编码。 business code
     * @param env     环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String configTag(Long tenantId, String bizCode, String env, String appCode) {
        return tag(tenantId(tenantId) + "\n" + bizCode + "\n" + env + "\n" + appCode);
    }

    /**
     * 从tenant和规范服务键计算服务作用域hash tag。 Computes a service-scope hash tag from tenant and canonical service key.
     *
     * @param serviceKey 规范服务键。 canonical service key
     * @param tenantId   存储租户标识。 storage tenant identifier
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String registryTag(Long tenantId, DdcServiceKey serviceKey) {
        return registryTag(
                tenantId,
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
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String registryTag(
            Long tenantId,
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind) {
        return tag(tenantId(tenantId) + "\n" + bizCode + "\n" + env
                + "\n" + appCode + "\n" + kind.name());
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
     * 组合tenant、版本和作用域tag生成Redis键。 Builds a Redis key with tenant, version, and scope tag.
     *
     * @param scopeTag Redis Cluster 作用域 tag。 Redis Cluster scope tag
     * @param parts    剩余键段。 remaining key segments
     * @param tenantId 存储租户标识。 storage tenant identifier
     * @return 完整 Redis 键。 complete Redis key
     */
    private static String buildKey(Long tenantId, String scopeTag, String... parts) {
        return PREFIX + ":" + KEY_VERSION + ":tenant:" + tenantId(tenantId)
                + ":" + scopeTag + ":" + String.join(":", parts);
    }

    private static long tenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be a positive Long");
        }
        return tenantId;
    }
}
