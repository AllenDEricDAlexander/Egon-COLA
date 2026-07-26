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

    public static String registryInstance(String env,
                                          String namespace,
                                          DdcServiceKind kind,
                                          String instanceId) {
        return join("lease", "instance", env, namespace, kind.name(), instanceId);
    }

    public static String registryService(DdcServiceKey serviceKey) {
        return join(
                "registry",
                "service",
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
                serviceKey.env(),
                serviceKey.namespace(),
                serviceKey.serviceKind().name(),
                digest(serviceKey)
        );
    }

    public static String registryCatalog(String env,
                                         String namespace,
                                         DdcServiceKind kind,
                                         String protocol) {
        return join("registry", "catalog", env, namespace, kind.name(), protocol);
    }

    public static String registryCatalogRevision(String env,
                                                 String namespace,
                                                 DdcServiceKind kind,
                                                 String protocol) {
        return join(
                "registry",
                "catalog-revision",
                env,
                namespace,
                kind.name(),
                protocol
        );
    }

    public static String registryTopic(String env,
                                       String namespace,
                                       DdcServiceKind kind,
                                       String protocol) {
        return join("registry", "topic", env, namespace, kind.name(), protocol);
    }

    public static String v2RegistryInstance(DdcServiceKey serviceKey, String instanceId) {
        return v2(
                registryTag(
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
                        serviceKey.env(),
                        serviceKey.namespace(),
                        serviceKey.serviceKind()
                ),
                "registry",
                "revision",
                digest(serviceKey.canonicalValue())
        );
    }

    public static String v2RegistryCatalog(String env,
                                           String namespace,
                                           DdcServiceKind kind,
                                           String protocol) {
        return v2(
                registryTag(env, namespace, kind),
                "registry",
                "catalog",
                protocol
        );
    }

    public static String v2RegistryCatalogRevision(String env,
                                                   String namespace,
                                                   DdcServiceKind kind,
                                                   String protocol) {
        return v2(
                registryTag(env, namespace, kind),
                "registry",
                "catalog-revision",
                protocol
        );
    }

    public static String v2RegistryTopic(String env,
                                         String namespace,
                                         DdcServiceKind kind,
                                         String protocol) {
        return v2(
                registryTag(env, namespace, kind),
                "registry",
                "topic",
                protocol
        );
    }

    private static String configTag(String appCode, String env, String namespace) {
        return tag(appCode + "\n" + env + "\n" + namespace);
    }

    private static String registryTag(String env,
                                      String namespace,
                                      DdcServiceKind kind) {
        return tag(env + "\n" + namespace + "\n" + kind.name());
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

    private static String join(String... parts) {
        return PREFIX + ":" + String.join(":", parts);
    }
}
