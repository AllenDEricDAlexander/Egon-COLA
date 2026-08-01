package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

public final class DdcKeys {

    private static final String PREFIX = "ddc";

    private DdcKeys() {
    }

    public static String config(String appCode, String env, String namespace, String key) {
        return join("config", appCode, env, namespace, key);
    }

    public static String version(String appCode, String env, String namespace, String key) {
        return join("version", appCode, env, namespace, key);
    }

    public static String instance(String appCode, String env, String namespace, String instanceId) {
        return join("instance", appCode, env, namespace, instanceId);
    }

    public static String instances(String appCode, String env, String namespace) {
        return join("instances", appCode, env, namespace);
    }

    public static String leaseInstance(String env,
                                       String namespace,
                                       DdcLeaseRole role,
                                       String instanceId) {
        return join("lease", "instance", env, namespace, role.name(), instanceId);
    }

    public static String publish(String changeId) {
        return join("publish", changeId);
    }

    public static String publishAck(String changeId) {
        return join("publish", "ack", changeId);
    }

    public static String topic(String appCode, String env, String namespace) {
        return join("topic", appCode, env, namespace);
    }

    public static String v2Config(String appCode,
                                  String env,
                                  String namespace,
                                  String key) {
        return v2(configTag(appCode, env, namespace), "config", key);
    }

    public static String v2Version(String appCode,
                                   String env,
                                   String namespace,
                                   String key) {
        return v2(configTag(appCode, env, namespace), "version", key);
    }

    public static String v2Topic(String appCode, String env, String namespace) {
        return v2(configTag(appCode, env, namespace), "topic");
    }

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

    public static String v2ConfigLeaseInstances(String appCode,
                                                String env,
                                                String namespace) {
        return v2(configTag(appCode, env, namespace), "lease", "instances");
    }

    public static String registryInstance(String bizCode,
                                          String appCode,
                                          String env,
                                          String namespace,
                                          DdcServiceKind kind,
                                          String instanceId) {
        return join("lease", "instance", bizCode, appCode, env, namespace, kind.name(), instanceId);
    }

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

    public static String registryCatalog(String bizCode,
                                         String appCode,
                                         String env,
                                         String namespace,
                                         DdcServiceKind kind,
                                         String protocol) {
        return join("registry", "catalog", bizCode, appCode, env, namespace, kind.name(), protocol);
    }

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

    public static String registryTopic(String bizCode,
                                       String appCode,
                                       String env,
                                       String namespace,
                                       DdcServiceKind kind,
                                       String protocol) {
        return join("registry", "topic", bizCode, appCode, env, namespace, kind.name(), protocol);
    }

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

    public static String v3Config(String bizCode, String env, String appCode, String key) {
        return v3(configV3Tag(bizCode, env, appCode), "config", key);
    }

    public static String v3Version(String bizCode, String env, String appCode, String key) {
        return v3(configV3Tag(bizCode, env, appCode), "version", key);
    }

    public static String v3Topic(String bizCode, String env, String appCode) {
        return v3(configV3Tag(bizCode, env, appCode), "topic");
    }

    public static String v3PublishIdempotency(
            String bizCode,
            String env,
            String appCode,
            String changeId) {
        return v3(configV3Tag(bizCode, env, appCode), "publish", "idempotency", changeId);
    }

    public static String v3ConfigLeaseInstance(
            String bizCode,
            String env,
            String appCode,
            String instanceId) {
        return v3(configV3Tag(bizCode, env, appCode), "lease", "instance", instanceId);
    }

    public static String v3ConfigLeaseInstances(String bizCode, String env, String appCode) {
        return v3(configV3Tag(bizCode, env, appCode), "lease", "instances");
    }

    public static String v3RegistryInstance(DdcServiceKey serviceKey, String instanceId) {
        return v3(registryV3Tag(serviceKey), "registry", "instance", instanceId);
    }

    public static String v3RegistryService(DdcServiceKey serviceKey) {
        return v3(registryV3Tag(serviceKey), "registry", "service", serviceKey.serviceId());
    }

    public static String v3RegistryRevision(DdcServiceKey serviceKey) {
        return v3(registryV3Tag(serviceKey), "registry", "revision", serviceKey.serviceId());
    }

    public static String v3RegistryCatalog(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return v3(registryV3Tag(bizCode, env, appCode, kind), "registry", "catalog", protocol);
    }

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

    public static String v3RegistryTopic(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind,
            String protocol) {
        return v3(registryV3Tag(bizCode, env, appCode, kind), "registry", "topic", protocol);
    }

    public static String v3GlobalRegistryCatalog() {
        return PREFIX + ":v3:{registry-catalog}:services";
    }

    public static String v3GlobalRegistryCatalogRevision() {
        return PREFIX + ":v3:{registry-catalog}:revision";
    }

    private static String configV3Tag(String bizCode, String env, String appCode) {
        return tag(bizCode + "\n" + env + "\n" + appCode);
    }

    private static String registryV3Tag(DdcServiceKey serviceKey) {
        return registryV3Tag(
                serviceKey.bizCode(),
                serviceKey.env(),
                serviceKey.appCode(),
                serviceKey.serviceKind()
        );
    }

    private static String registryV3Tag(
            String bizCode,
            String env,
            String appCode,
            DdcServiceKind kind) {
        return tag(bizCode + "\n" + env + "\n" + appCode + "\n" + kind.name());
    }

    private static String configTag(String appCode, String env, String namespace) {
        return tag(appCode + "\n" + env + "\n" + namespace);
    }

    private static String registryTag(String bizCode,
                                      String appCode,
                                      String env,
                                      String namespace,
                                      DdcServiceKind kind) {
        return tag(bizCode + "\n" + appCode + "\n" + env + "\n" + namespace + "\n" + kind.name());
    }

    private static String tag(String value) {
        return "{" + digest(value) + "}";
    }

    private static String digest(DdcServiceKey serviceKey) {
        return digest(serviceKey.canonicalValue());
    }

    private static String digest(String value) {
        return Digests.sha256Hex(value);
    }

    private static String v2(String scopeTag, String... parts) {
        return PREFIX + ":v2:" + scopeTag + ":" + String.join(":", parts);
    }

    private static String v3(String scopeTag, String... parts) {
        return PREFIX + ":v3:" + scopeTag + ":" + String.join(":", parts);
    }

    private static String join(String... parts) {
        return PREFIX + ":" + String.join(":", parts);
    }
}
