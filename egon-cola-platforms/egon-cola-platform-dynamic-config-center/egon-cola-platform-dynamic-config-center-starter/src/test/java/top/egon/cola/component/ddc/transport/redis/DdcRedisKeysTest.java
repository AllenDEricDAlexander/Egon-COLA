package top.egon.cola.component.ddc.transport.redis;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRedisKeysTest {

    @Test
    void exposesVersionNeutralPublicMethods() {
        assertThat(Arrays.stream(DdcRedisKeys.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName()))
                .noneMatch(name -> name.matches("v\\d+.*"));
    }

    @Test
    void buildsConfigKeysWithoutNamespace() {
        String value = DdcRedisKeys.config(
                "retail", "local", "order", "feature.enabled"
        );
        String version = DdcRedisKeys.version(
                "retail", "local", "order", "feature.enabled"
        );
        String topic = DdcRedisKeys.topic("retail", "local", "order");
        String lease = DdcRedisKeys.configLeaseInstance(
                "retail", "local", "order", "instance-1"
        );
        String leases = DdcRedisKeys.configLeaseInstances(
                "retail", "local", "order"
        );
        String idempotency = DdcRedisKeys.publishIdempotency(
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

        String instance = DdcRedisKeys.registryInstance(key, "instance-1");
        String service = DdcRedisKeys.registryService(key);
        String revision = DdcRedisKeys.registryRevision(key);
        String catalog = DdcRedisKeys.registryCatalog(
                "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );
        String catalogRevision = DdcRedisKeys.registryCatalogRevision(
                "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );
        String topic = DdcRedisKeys.registryTopic(
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
        assertThat(DdcRedisKeys.globalRegistryCatalog())
                .isEqualTo("ddc:v3:{registry-catalog}:services");
        assertThat(DdcRedisKeys.globalRegistryCatalogRevision())
                .isEqualTo("ddc:v3:{registry-catalog}:revision");
    }

    private String hashTag(String key) {
        return key.substring(key.indexOf('{') + 1, key.indexOf('}'));
    }
}
