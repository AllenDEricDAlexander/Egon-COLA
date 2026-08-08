package top.egon.cola.component.ddc.common;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DdcKeysTest {

    @Test
    void exposesOnlyV3PublicMethods() {
        assertThat(Arrays.stream(DdcKeys.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName()))
                .allMatch(name -> name.startsWith("v3"));
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
        String leases = DdcKeys.v3ConfigLeaseInstances(
                "retail", "local", "order"
        );
        String idempotency = DdcKeys.v3PublishIdempotency(
                "retail", "local", "order", "change-1"
        );

        assertThat(hashTag(value))
                .isEqualTo(hashTag(version))
                .isEqualTo(hashTag(topic))
                .isEqualTo(hashTag(lease))
                .isEqualTo(hashTag(leases))
                .isEqualTo(hashTag(idempotency));
        assertThat(hashTag(value))
                .hasSize(64)
                .doesNotContain("retail", "local", "order");
        assertThat(value).startsWith("ddc:v3:{").endsWith(":config:feature.enabled");
        assertThat(version).endsWith(":version:feature.enabled");
        assertThat(topic).endsWith(":topic");
        assertThat(lease).endsWith(":lease:instance:instance-1");
        assertThat(leases).endsWith(":lease:instances");
        assertThat(idempotency).endsWith(":publish:idempotency:change-1");
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

        String instance = DdcKeys.v3RegistryInstance(key, "instance-1");
        String service = DdcKeys.v3RegistryService(key);
        String revision = DdcKeys.v3RegistryRevision(key);
        String catalog = DdcKeys.v3RegistryCatalog(
                "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );
        String catalogRevision = DdcKeys.v3RegistryCatalogRevision(
                "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );
        String topic = DdcKeys.v3RegistryTopic(
                "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );

        assertThat(hashTag(instance))
                .isEqualTo(hashTag(service))
                .isEqualTo(hashTag(revision))
                .isEqualTo(hashTag(catalog))
                .isEqualTo(hashTag(catalogRevision))
                .isEqualTo(hashTag(topic));
        assertThat(service).endsWith(":registry:service:" + key.serviceId());
        assertThat(revision).endsWith(":registry:revision:" + key.serviceId());
        assertThat(catalog).endsWith(":registry:catalog:http");
        assertThat(catalogRevision).endsWith(":registry:catalog-revision:http");
        assertThat(topic).endsWith(":registry:topic:http");
        assertThat(DdcKeys.v3GlobalRegistryCatalog())
                .isEqualTo("ddc:v3:{registry-catalog}:services");
        assertThat(DdcKeys.v3GlobalRegistryCatalogRevision())
                .isEqualTo("ddc:v3:{registry-catalog}:revision");
    }

    private String hashTag(String key) {
        return key.substring(key.indexOf('{') + 1, key.indexOf('}'));
    }
}
