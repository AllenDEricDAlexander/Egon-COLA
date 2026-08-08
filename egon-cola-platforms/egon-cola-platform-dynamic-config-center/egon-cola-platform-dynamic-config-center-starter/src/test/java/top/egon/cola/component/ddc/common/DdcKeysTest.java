package top.egon.cola.component.ddc.common;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DdcKeysTest {

    @Test
    void exposesVersionNeutralPublicMethods() {
        assertThat(Arrays.stream(DdcKeys.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName()))
                .noneMatch(name -> name.matches("v\\d+.*"));
    }

    @Test
    void buildsConfigKeysWithoutNamespace() {
        String value = DdcKeys.config(
                "retail", "local", "order", "feature.enabled"
        );
        String version = DdcKeys.version(
                "retail", "local", "order", "feature.enabled"
        );
        String topic = DdcKeys.topic("retail", "local", "order");
        String lease = DdcKeys.configLeaseInstance(
                "retail", "local", "order", "instance-1"
        );
        String leases = DdcKeys.configLeaseInstances(
                "retail", "local", "order"
        );
        String idempotency = DdcKeys.publishIdempotency(
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
    void buildsRegistryKeysAndIndependentGlobalCatalog() {
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

        String instance = DdcKeys.registryInstance(key, "instance-1");
        String service = DdcKeys.registryService(key);
        String revision = DdcKeys.registryRevision(key);
        String catalog = DdcKeys.registryCatalog(
                "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );
        String catalogRevision = DdcKeys.registryCatalogRevision(
                "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );
        String topic = DdcKeys.registryTopic(
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
        assertThat(DdcKeys.globalRegistryCatalog())
                .isEqualTo("ddc:v3:{registry-catalog}:services");
        assertThat(DdcKeys.globalRegistryCatalogRevision())
                .isEqualTo("ddc:v3:{registry-catalog}:revision");
    }

    private String hashTag(String key) {
        return key.substring(key.indexOf('{') + 1, key.indexOf('}'));
    }
}
