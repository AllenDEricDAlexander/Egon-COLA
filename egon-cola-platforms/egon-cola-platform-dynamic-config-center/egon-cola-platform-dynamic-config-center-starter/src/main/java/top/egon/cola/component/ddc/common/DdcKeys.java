package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

/**
 * 生成 DDC 配置、租约、发布和服务注册使用的 Redis 键。 Generates Redis keys used by DDC configuration, leases, publications, and service registry.
 */
public final class DdcKeys {

    /** 所有 DDC Redis 键的固定前缀。 Fixed prefix for all DDC Redis keys. */
    private static final String PREFIX = "ddc";

    /** 禁止实例化键生成工具类。 Prevents instantiation of the key-generation utility. */
    private DdcKeys() {
    }

    /**
     * 生成旧版配置值键。 Generates a legacy configuration-value key.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param key 配置键。 configuration key
     * @return 旧版 Redis 键。 legacy Redis key
     */
    public static String config(String appCode, String env, String namespace, String key) {
        return join("config", appCode, env, namespace, key);
    }

    /**
     * 生成旧版配置版本键。 Generates a legacy configuration-version key.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param key 配置键。 configuration key
     * @return 旧版版本 Redis 键。 legacy version Redis key
     */
    public static String version(String appCode, String env, String namespace, String key) {
        return join("version", appCode, env, namespace, key);
    }

    /**
     * 生成旧版配置实例键。 Generates a legacy configuration-instance key.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param instanceId 实例标识。 instance identifier
     * @return 旧版实例 Redis 键。 legacy instance Redis key
     */
    public static String instance(String appCode, String env, String namespace, String instanceId) {
        return join("instance", appCode, env, namespace, instanceId);
    }

    /**
     * 生成旧版配置实例集合键。 Generates a legacy configuration-instance collection key.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @return 旧版实例集合 Redis 键。 legacy instance-collection Redis key
     */
    public static String instances(String appCode, String env, String namespace) {
        return join("instances", appCode, env, namespace);
    }

    /**
     * 生成按环境、命名空间和租约角色定位的旧版实例租约键。 Generates a legacy instance-lease key scoped by environment, namespace, and lease role.
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param role 租约角色。 lease role
     * @param instanceId 实例标识。 instance identifier
     * @return 旧版实例租约 Redis 键。 legacy instance-lease Redis key
     */
    public static String leaseInstance(String env,
                                       String namespace,
                                       DdcLeaseRole role,
                                       String instanceId) {
        return join("lease", "instance", env, namespace, role.name(), instanceId);
    }

    /**
     * 生成旧版发布状态键。 Generates a legacy publication-state key.
     * @param changeId 变更标识。 change identifier
     * @return 旧版发布 Redis 键。 legacy publication Redis key
     */
    public static String publish(String changeId) {
        return join("publish", changeId);
    }

    /**
     * 生成旧版发布确认键。 Generates a legacy publication-acknowledgement key.
     * @param changeId 变更标识。 change identifier
     * @return 旧版发布确认 Redis 键。 legacy publication-acknowledgement Redis key
     */
    public static String publishAck(String changeId) {
        return join("publish", "ack", changeId);
    }

    /**
     * 生成旧版配置变更 Topic 名称。 Generates a legacy configuration-change topic name.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @return 旧版 Redis Topic 名称。 legacy Redis topic name
     */
    public static String topic(String appCode, String env, String namespace) {
        return join("topic", appCode, env, namespace);
    }

    /**
     * 生成使用作用域 hash tag 的 v2 配置值键。 Generates a v2 configuration-value key using a scope hash tag.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param key 配置键。 configuration key
     * @return v2 配置 Redis 键。 v2 configuration Redis key
     */
    public static String v2Config(String appCode,
                                  String env,
                                  String namespace,
                                  String key) {
        return v2(configTag(appCode, env, namespace), "config", key);
    }

    /**
     * 生成使用作用域 hash tag 的 v2 配置版本键。 Generates a v2 configuration-version key using a scope hash tag.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param key 配置键。 configuration key
     * @return v2 版本 Redis 键。 v2 version Redis key
     */
    public static String v2Version(String appCode,
                                   String env,
                                   String namespace,
                                   String key) {
        return v2(configTag(appCode, env, namespace), "version", key);
    }

    /**
     * 生成使用作用域 hash tag 的 v2 配置变更 Topic。 Generates a v2 configuration-change topic using a scope hash tag.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @return v2 Redis Topic 名称。 v2 Redis topic name
     */
    public static String v2Topic(String appCode, String env, String namespace) {
        return v2(configTag(appCode, env, namespace), "topic");
    }

    /**
     * 生成 v2 发布幂等记录键，使同一配置作用域落入同一槽位。 Generates a v2 publication-idempotency key that keeps one configuration scope in the same slot.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param changeId 变更标识。 change identifier
     * @return v2 发布幂等 Redis 键。 v2 publication-idempotency Redis key
     */
    public static String v2PublishIdempotency(String appCode,
                                              String env,
                                              String namespace,
                                              String changeId) {
        return v2(
                configTag(appCode, env, namespace),
                "publish",
                "idempotency",
                changeId
        );
    }

    /**
     * 生成 v2 单实例配置租约键。 Generates a v2 per-instance configuration-lease key.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param instanceId 实例标识。 instance identifier
     * @return v2 配置租约 Redis 键。 v2 configuration-lease Redis key
     */
    public static String v2ConfigLeaseInstance(String appCode,
                                               String env,
                                               String namespace,
                                               String instanceId) {
        return v2(
                configTag(appCode, env, namespace),
                "lease",
                "instance",
                instanceId
        );
    }

    /**
     * 生成 v2 配置租约实例集合键。 Generates a v2 configuration-lease instance collection key.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @return v2 配置租约实例集合 Redis 键。 v2 configuration-lease instance collection Redis key
     */
    public static String v2ConfigLeaseInstances(String appCode,
                                                String env,
                                                String namespace) {
        return v2(configTag(appCode, env, namespace), "lease", "instances");
    }

    /**
     * 生成旧版服务注册实例租约键。 Generates a legacy service-registry instance-lease key.
     * @param bizCode 业务编码。 business code
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param kind 服务类型。 service kind
     * @param instanceId 实例标识。 instance identifier
     * @return 旧版注册实例 Redis 键。 legacy registry-instance Redis key
     */
    public static String registryInstance(String bizCode,
                                          String appCode,
                                          String env,
                                          String namespace,
                                          DdcServiceKind kind,
                                          String instanceId) {
        return join("lease", "instance", bizCode, appCode, env, namespace, kind.name(), instanceId);
    }

    /**
     * 生成旧版服务定义键，并以规范服务键摘要区分具体服务。 Generates a legacy service-definition key distinguished by the canonical service-key digest.
     * @param serviceKey 规范服务键。 canonical service key
     * @return 旧版服务定义 Redis 键。 legacy service-definition Redis key
     */
    public static String registryService(DdcServiceKey serviceKey) {
        return join(
                "registry",
                "service",
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env(),
                serviceKey.namespace(),
                serviceKey.serviceKind().name(),
                digest(serviceKey)
        );
    }

    /**
     * 生成旧版服务修订版本键。 Generates a legacy service-revision key.
     * @param serviceKey 规范服务键。 canonical service key
     * @return 旧版服务修订 Redis 键。 legacy service-revision Redis key
     */
    public static String registryRevision(DdcServiceKey serviceKey) {
        return join(
                "registry",
                "revision",
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env(),
                serviceKey.namespace(),
                serviceKey.serviceKind().name(),
                digest(serviceKey)
        );
    }

    /**
     * 生成旧版协议服务目录键。 Generates a legacy protocol-specific service-catalog key.
     * @param bizCode 业务编码。 business code
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param kind 服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return 旧版服务目录 Redis 键。 legacy service-catalog Redis key
     */
    public static String registryCatalog(String bizCode,
                                         String appCode,
                                         String env,
                                         String namespace,
                                         DdcServiceKind kind,
                                         String protocol) {
        return join("registry", "catalog", bizCode, appCode, env, namespace, kind.name(), protocol);
    }

    /**
     * 生成旧版协议服务目录修订键。 Generates a legacy protocol-specific service-catalog revision key.
     * @param bizCode 业务编码。 business code
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param kind 服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return 旧版目录修订 Redis 键。 legacy catalog-revision Redis key
     */
    public static String registryCatalogRevision(String bizCode,
                                                 String appCode,
                                                 String env,
                                                 String namespace,
                                                 DdcServiceKind kind,
                                                 String protocol) {
        return join(
                "registry",
                "catalog-revision",
                bizCode,
                appCode,
                env,
                namespace,
                kind.name(),
                protocol
        );
    }

    /**
     * 生成旧版协议服务注册变更 Topic。 Generates a legacy protocol-specific registry-change topic.
     * @param bizCode 业务编码。 business code
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param kind 服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return 旧版注册变更 Topic 名称。 legacy registry-change topic name
     */
    public static String registryTopic(String bizCode,
                                       String appCode,
                                       String env,
                                       String namespace,
                                       DdcServiceKind kind,
                                       String protocol) {
        return join("registry", "topic", bizCode, appCode, env, namespace, kind.name(), protocol);
    }

    /**
     * 生成带服务作用域 hash tag 的 v2 注册实例键。 Generates a v2 registry-instance key with a service-scope hash tag.
     * @param serviceKey 规范服务键。 canonical service key
     * @param instanceId 实例标识。 instance identifier
     * @return v2 注册实例 Redis 键。 v2 registry-instance Redis key
     */
    public static String v2RegistryInstance(DdcServiceKey serviceKey, String instanceId) {
        return v2(
                registryTag(
                        serviceKey.bizCode(),
                        serviceKey.appCode(),
                        serviceKey.env(),
                        serviceKey.namespace(),
                        serviceKey.serviceKind()
                ),
                "registry",
                "instance",
                instanceId
        );
    }

    /**
     * 生成带服务作用域 hash tag 的 v2 服务定义键。 Generates a v2 service-definition key with a service-scope hash tag.
     * @param serviceKey 规范服务键。 canonical service key
     * @return v2 服务定义 Redis 键。 v2 service-definition Redis key
     */
    public static String v2RegistryService(DdcServiceKey serviceKey) {
        return v2(
                registryTag(
                        serviceKey.bizCode(),
                        serviceKey.appCode(),
                        serviceKey.env(),
                        serviceKey.namespace(),
                        serviceKey.serviceKind()
                ),
                "registry",
                "service",
                digest(serviceKey.canonicalValue())
        );
    }

    /**
     * 生成带服务作用域 hash tag 的 v2 服务修订键。 Generates a v2 service-revision key with a service-scope hash tag.
     * @param serviceKey 规范服务键。 canonical service key
     * @return v2 服务修订 Redis 键。 v2 service-revision Redis key
     */
    public static String v2RegistryRevision(DdcServiceKey serviceKey) {
        return v2(
                registryTag(
                        serviceKey.bizCode(),
                        serviceKey.appCode(),
                        serviceKey.env(),
                        serviceKey.namespace(),
                        serviceKey.serviceKind()
                ),
                "registry",
                "revision",
                digest(serviceKey.canonicalValue())
        );
    }

    /**
     * 生成 v2 协议服务目录键。 Generates a v2 protocol-specific service-catalog key.
     * @param bizCode 业务编码。 business code
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param kind 服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return v2 服务目录 Redis 键。 v2 service-catalog Redis key
     */
    public static String v2RegistryCatalog(String bizCode,
                                           String appCode,
                                           String env,
                                           String namespace,
                                           DdcServiceKind kind,
                                           String protocol) {
        return v2(
                registryTag(bizCode, appCode, env, namespace, kind),
                "registry",
                "catalog",
                protocol
        );
    }

    /**
     * 生成 v2 协议服务目录修订键。 Generates a v2 protocol-specific service-catalog revision key.
     * @param bizCode 业务编码。 business code
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param kind 服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return v2 目录修订 Redis 键。 v2 catalog-revision Redis key
     */
    public static String v2RegistryCatalogRevision(String bizCode,
                                                   String appCode,
                                                   String env,
                                                   String namespace,
                                                   DdcServiceKind kind,
                                                   String protocol) {
        return v2(
                registryTag(bizCode, appCode, env, namespace, kind),
                "registry",
                "catalog-revision",
                protocol
        );
    }

    /**
     * 生成 v2 协议服务注册变更 Topic。 Generates a v2 protocol-specific registry-change topic.
     * @param bizCode 业务编码。 business code
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param kind 服务类型。 service kind
     * @param protocol 服务协议。 service protocol
     * @return v2 注册变更 Topic 名称。 v2 registry-change topic name
     */
    public static String v2RegistryTopic(String bizCode,
                                         String appCode,
                                         String env,
                                         String namespace,
                                         DdcServiceKind kind,
                                         String protocol) {
        return v2(
                registryTag(bizCode, appCode, env, namespace, kind),
                "registry",
                "topic",
                protocol
        );
    }

    /**
     * 生成以业务、环境和应用为作用域的 v3 配置值键。 Generates a v3 configuration-value key scoped by business, environment, and application.
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param key 配置键。 configuration key
     * @return v3 配置 Redis 键。 v3 configuration Redis key
     */
    public static String v3Config(String bizCode, String env, String appCode, String key) {
        return v3(configV3Tag(bizCode, env, appCode), "config", key);
    }

    /**
     * 生成 v3 配置版本键。 Generates a v3 configuration-version key.
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param key 配置键。 configuration key
     * @return v3 版本 Redis 键。 v3 version Redis key
     */
    public static String v3Version(String bizCode, String env, String appCode, String key) {
        return v3(configV3Tag(bizCode, env, appCode), "version", key);
    }

    /**
     * 生成 v3 配置变更 Topic。 Generates a v3 configuration-change topic.
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return v3 配置变更 Topic 名称。 v3 configuration-change topic name
     */
    public static String v3Topic(String bizCode, String env, String appCode) {
        return v3(configV3Tag(bizCode, env, appCode), "topic");
    }

    /**
     * 生成 v3 发布幂等记录键。 Generates a v3 publication-idempotency key.
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
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
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
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
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return v3 租约实例集合 Redis 键。 v3 lease-instance collection Redis key
     */
    public static String v3ConfigLeaseInstances(String bizCode, String env, String appCode) {
        return v3(configV3Tag(bizCode, env, appCode), "lease", "instances");
    }

    /**
     * 生成以 v3 服务作用域 hash tag 定位的注册实例键。 Generates a registry-instance key located by the v3 service-scope hash tag.
     * @param serviceKey 规范服务键。 canonical service key
     * @param instanceId 实例标识。 instance identifier
     * @return v3 注册实例 Redis 键。 v3 registry-instance Redis key
     */
    public static String v3RegistryInstance(DdcServiceKey serviceKey, String instanceId) {
        return v3(registryV3Tag(serviceKey), "registry", "instance", instanceId);
    }

    /**
     * 生成以稳定服务标识结尾的 v3 服务定义键。 Generates a v3 service-definition key ending in the stable service identifier.
     * @param serviceKey 规范服务键。 canonical service key
     * @return v3 服务定义 Redis 键。 v3 service-definition Redis key
     */
    public static String v3RegistryService(DdcServiceKey serviceKey) {
        return v3(registryV3Tag(serviceKey), "registry", "service", serviceKey.serviceId());
    }

    /**
     * 生成以稳定服务标识结尾的 v3 服务修订键。 Generates a v3 service-revision key ending in the stable service identifier.
     * @param serviceKey 规范服务键。 canonical service key
     * @return v3 服务修订 Redis 键。 v3 service-revision Redis key
     */
    public static String v3RegistryRevision(DdcServiceKey serviceKey) {
        return v3(registryV3Tag(serviceKey), "registry", "revision", serviceKey.serviceId());
    }

    /**
     * 生成 v3 协议服务目录键。 Generates a v3 protocol-specific service-catalog key.
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param kind 服务类型。 service kind
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
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param kind 服务类型。 service kind
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
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param kind 服务类型。 service kind
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
     * @return 固定的全局服务目录 Redis 键。 fixed global service-catalog Redis key
     */
    public static String v3GlobalRegistryCatalog() {
        return PREFIX + ":v3:{registry-catalog}:services";
    }

    /**
     * 生成跨作用域的 v3 全局服务目录修订键。 Generates the cross-scope v3 global service-catalog revision key.
     * @return 固定的全局目录修订 Redis 键。 fixed global catalog-revision Redis key
     */
    public static String v3GlobalRegistryCatalogRevision() {
        return PREFIX + ":v3:{registry-catalog}:revision";
    }

    /**
     * 计算 v3 配置作用域的 Redis hash tag。 Computes the Redis hash tag for a v3 configuration scope.
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String configV3Tag(String bizCode, String env, String appCode) {
        return tag(bizCode + "\n" + env + "\n" + appCode);
    }

    /**
     * 从规范服务键计算 v3 服务作用域 hash tag。 Computes a v3 service-scope hash tag from a canonical service key.
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
     * @param bizCode 业务编码。 business code
     * @param env 环境编码。 environment code
     * @param appCode 应用编码。 application code
     * @param kind 服务类型。 service kind
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
     * 计算 v2 配置作用域的 Redis hash tag。 Computes the Redis hash tag for a v2 configuration scope.
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String configTag(String appCode, String env, String namespace) {
        return tag(appCode + "\n" + env + "\n" + namespace);
    }

    /**
     * 计算 v2 服务注册作用域的 Redis hash tag。 Computes the Redis hash tag for a v2 service-registry scope.
     * @param bizCode 业务编码。 business code
     * @param appCode 应用编码。 application code
     * @param env 环境编码。 environment code
     * @param namespace 命名空间编码。 namespace code
     * @param kind 服务类型。 service kind
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String registryTag(String bizCode,
                                      String appCode,
                                      String env,
                                      String namespace,
                                      DdcServiceKind kind) {
        return tag(bizCode + "\n" + appCode + "\n" + env + "\n" + namespace + "\n" + kind.name());
    }

    /**
     * 将作用域文本摘要包装为 Redis Cluster hash tag。 Wraps a scope-text digest as a Redis Cluster hash tag.
     * @param value 待摘要的规范作用域文本。 canonical scope text to digest
     * @return 带花括号的摘要 tag。 digest tag enclosed in braces
     */
    private static String tag(String value) {
        return "{" + digest(value) + "}";
    }

    /**
     * 计算规范服务键的 SHA-256 摘要。 Computes the SHA-256 digest of a canonical service key.
     * @param serviceKey 规范服务键。 canonical service key
     * @return 十六进制摘要。 hexadecimal digest
     */
    private static String digest(DdcServiceKey serviceKey) {
        return digest(serviceKey.canonicalValue());
    }

    /**
     * 计算文本的 SHA-256 摘要。 Computes the SHA-256 digest of text.
     * @param value 待摘要文本。 text to digest
     * @return 十六进制摘要。 hexadecimal digest
     */
    private static String digest(String value) {
        return Digests.sha256Hex(value);
    }

    /**
     * 组合带版本和作用域 tag 的 v2 Redis 键。 Joins a versioned v2 Redis key with its scope tag.
     * @param scopeTag Redis Cluster 作用域 tag。 Redis Cluster scope tag
     * @param parts 剩余键段。 remaining key segments
     * @return 完整 v2 Redis 键。 complete v2 Redis key
     */
    private static String v2(String scopeTag, String... parts) {
        return PREFIX + ":v2:" + scopeTag + ":" + String.join(":", parts);
    }

    /**
     * 组合带版本和作用域 tag 的 v3 Redis 键。 Joins a versioned v3 Redis key with its scope tag.
     * @param scopeTag Redis Cluster 作用域 tag。 Redis Cluster scope tag
     * @param parts 剩余键段。 remaining key segments
     * @return 完整 v3 Redis 键。 complete v3 Redis key
     */
    private static String v3(String scopeTag, String... parts) {
        return PREFIX + ":v3:" + scopeTag + ":" + String.join(":", parts);
    }

    /**
     * 使用 DDC 前缀和冒号连接旧版键段。 Joins legacy key segments with the DDC prefix and colons.
     * @param parts 键段。 key segments
     * @return 完整旧版 Redis 键。 complete legacy Redis key
     */
    private static String join(String... parts) {
        return PREFIX + ":" + String.join(":", parts);
    }
}
