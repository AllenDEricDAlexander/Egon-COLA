package top.egon.cola.component.ddc.common;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

import static org.assertj.core.api.Assertions.assertThat;

class DdcKeysTest {

    @Test
    void buildsConfigKeys() {
        assertThat(DdcKeys.config("demo", "dev", "default", "switch"))
                .isEqualTo("ddc:config:demo:dev:default:switch");
        assertThat(DdcKeys.version("demo", "dev", "default", "switch"))
                .isEqualTo("ddc:version:demo:dev:default:switch");
        assertThat(DdcKeys.topic("demo", "dev", "default"))
                .isEqualTo("ddc:topic:demo:dev:default");
    }

    @Test
    void buildsInstanceAndPublishKeys() {
        assertThat(DdcKeys.instance("demo", "dev", "default", "i1"))
                .isEqualTo("ddc:instance:demo:dev:default:i1");
        assertThat(DdcKeys.instances("demo", "dev", "default"))
                .isEqualTo("ddc:instances:demo:dev:default");
        assertThat(DdcKeys.leaseInstance("dev", "default", DdcLeaseRole.CONFIG_CLIENT, "i1"))
                .isEqualTo("ddc:lease:instance:dev:default:CONFIG_CLIENT:i1");
        assertThat(DdcKeys.publish("c1")).isEqualTo("ddc:publish:c1");
        assertThat(DdcKeys.publishAck("c1")).isEqualTo("ddc:publish:ack:c1");
    }

    @Test
    void buildsClusterSafeConfigKeysWithOneOpaqueScopeTag() {
        String value = DdcKeys.v2Config("demo", "dev", "default", "switch");
        String version = DdcKeys.v2Version("demo", "dev", "default", "switch");
        String topic = DdcKeys.v2Topic("demo", "dev", "default");
        String lease = DdcKeys.v2ConfigLeaseInstance(
                "demo", "dev", "default", "instance-1"
        );
        String instances = DdcKeys.v2ConfigLeaseInstances("demo", "dev", "default");

        assertThat(value).startsWith("ddc:v2:{").endsWith(":config:switch");
        assertThat(hashTag(value)).hasSize(64).doesNotContain("demo", "dev", "default");
        assertThat(hashTag(version))
                .isEqualTo(hashTag(value))
                .isEqualTo(hashTag(topic))
                .isEqualTo(hashTag(lease))
                .isEqualTo(hashTag(instances));
    }

    @Test
    void buildsClusterSafeRegistryKeysWithOneKindScopeTag() {
        DdcServiceKey serviceKey = new DdcServiceKey(
                        "pay-biz",
                        "orders-app",
                        "dev",
                        "default",
                        DdcServiceKind.RPC_PROVIDER,
                "order.v1.OrderQueryService",
                "default",
                "1.0.0",
                "grpc"
        );

        assertThat(hashTag(DdcKeys.v2RegistryInstance(serviceKey, "provider-1")))
                .isEqualTo(hashTag(DdcKeys.v2RegistryService(serviceKey)))
                .isEqualTo(hashTag(DdcKeys.v2RegistryRevision(serviceKey)))
                .isEqualTo(hashTag(DdcKeys.v2RegistryCatalog(
                        "pay-biz", "orders-app", "dev", "", DdcServiceKind.RPC_PROVIDER, "grpc"
                )))
                .isEqualTo(hashTag(DdcKeys.v2RegistryCatalogRevision(
                        "pay-biz", "orders-app", "dev", "", DdcServiceKind.RPC_PROVIDER, "grpc"
                )))
                .isEqualTo(hashTag(DdcKeys.v2RegistryTopic(
                        "pay-biz", "orders-app", "dev", "", DdcServiceKind.RPC_PROVIDER, "grpc"
                )));
    }

    @Test
    void buildsV3ConfigKeysWithoutNamespace() {
        String value = DdcKeys.v3Config(
                "retail", "local", "order", "feature.enabled"
        );
        String version = DdcKeys.v3Version(
                "retail", "local", "order", "feature.enabled"
        );
        String topic = DdcKeys.v3Topic("retail", "local", "order");
        String lease = DdcKeys.v3ConfigLeaseInstance(
                "retail", "local", "order", "instance-1"
        );

        assertThat(hashTag(value))
                .isEqualTo(hashTag(version))
                .isEqualTo(hashTag(topic))
                .isEqualTo(hashTag(lease));
        assertThat(value).startsWith("ddc:v3:{").endsWith(":config:feature.enabled");
    }

    @Test
    void buildsV3RegistryKeysAndIndependentGlobalCatalog() {
        DdcServiceKey key = new DdcServiceKey(
                "retail",
                "local",
                "order",
                DdcServiceKind.HTTP_PROVIDER,
                "order-service",
                "default",
                "1.0.0",
                "http"
        );

        assertThat(hashTag(DdcKeys.v3RegistryInstance(key, "instance-1")))
                .isEqualTo(hashTag(DdcKeys.v3RegistryService(key)))
                .isEqualTo(hashTag(DdcKeys.v3RegistryRevision(key)))
                .isEqualTo(hashTag(DdcKeys.v3RegistryCatalog(
                        "retail", "local", "order",
                        DdcServiceKind.HTTP_PROVIDER, "http"
                )));
        assertThat(DdcKeys.v3GlobalRegistryCatalog())
                .isEqualTo("ddc:v3:{registry-catalog}:services");
        assertThat(DdcKeys.v3GlobalRegistryCatalogRevision())
                .isEqualTo("ddc:v3:{registry-catalog}:revision");
    }

    private String hashTag(String key) {
        return key.substring(key.indexOf('{') + 1, key.indexOf('}'));
    }
}
