package top.egon.cola.component.ddc.common;

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

    public static String publish(String changeId) {
        return join("publish", changeId);
    }

    public static String publishAck(String changeId) {
        return join("publish", "ack", changeId);
    }

    public static String topic(String appCode, String env, String namespace) {
        return join("topic", appCode, env, namespace);
    }

    private static String join(String... parts) {
        return PREFIX + ":" + String.join(":", parts);
    }
}
