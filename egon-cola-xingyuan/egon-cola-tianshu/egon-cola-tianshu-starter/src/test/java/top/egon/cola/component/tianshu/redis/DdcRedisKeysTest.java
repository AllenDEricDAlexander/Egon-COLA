package top.egon.cola.component.tianshu.redis;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                101L, "retail", "local", "order", "feature.enabled"
        );
        String version = DdcRedisKeys.version(
                101L, "retail", "local", "order", "feature.enabled"
        );
        String topic = DdcRedisKeys.topic(101L, "retail", "local", "order");
        String lease = DdcRedisKeys.configLeaseInstance(
                101L, "retail", "local", "order", "instance-1"
        );
        String leases = DdcRedisKeys.configLeaseInstances(
                101L, "retail", "local", "order"
        );
        String idempotency = DdcRedisKeys.publishIdempotency(
                101L, "retail", "local", "order", "change-1"
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
        assertThat(value).startsWith("tianshu:v4:tenant:101:{").endsWith(":config:feature.enabled");
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

        String instance = DdcRedisKeys.registryInstance(101L, key, "instance-1");
        String service = DdcRedisKeys.registryService(101L, key);
        String revision = DdcRedisKeys.registryRevision(101L, key);
        String catalog = DdcRedisKeys.registryCatalog(
                101L, "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );
        String catalogRevision = DdcRedisKeys.registryCatalogRevision(
                101L, "retail", "local", "order",
                DdcServiceKind.HTTP_PROVIDER, "http"
        );
        String topic = DdcRedisKeys.registryTopic(
                101L, "retail", "local", "order",
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
        assertThat(DdcRedisKeys.globalRegistryCatalog(101L))
                .startsWith("tianshu:v4:tenant:101:{")
                .endsWith(":services");
        assertThat(DdcRedisKeys.globalRegistryCatalogRevision(101L))
                .startsWith("tianshu:v4:tenant:101:{")
                .endsWith(":revision");
    }

    @Test
    void sameScopeInDifferentTenantsUsesDifferentSlotsAndCatalogs() {
        String configA = DdcRedisKeys.config(101L, "retail", "dev", "order", "switch");
        String configB = DdcRedisKeys.config(202L, "retail", "dev", "order", "switch");
        String globalA = DdcRedisKeys.globalRegistryCatalog(101L);
        String globalB = DdcRedisKeys.globalRegistryCatalog(202L);

        assertThat(configA).isNotEqualTo(configB);
        assertThat(hashTag(configA)).isNotEqualTo(hashTag(configB));
        assertThat(globalA).isNotEqualTo(globalB);
        assertThat(hashTag(globalA)).isNotEqualTo(hashTag(globalB));
    }

    @Test
    void rejectsMissingAndNonPositiveTenantIds() {
        assertThatThrownBy(() ->
                DdcRedisKeys.config(null, "retail", "dev", "order", "switch"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                DdcRedisKeys.topic(0L, "retail", "dev", "order"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aiTopicUsesTenantScopedV4PrefixAndAiSuffix() {
        String aiTopic = DdcRedisKeys.aiTopic(42L, "xingyuan", "dev", "order-service");
        String configTopic = DdcRedisKeys.topic(42L, "xingyuan", "dev", "order-service");

        assertThat(aiTopic).matches("^tianshu:v4:tenant:42:\\{[a-f0-9]{64}}:ai:topic$");
        assertThat(hashTag(aiTopic)).isEqualTo(hashTag(configTopic));
        assertThat(aiTopic).isNotEqualTo(configTopic);
        assertThat(hashTag(DdcRedisKeys.aiTopic(43L, "xingyuan", "dev", "order-service")))
                .isNotEqualTo(hashTag(aiTopic));
    }

    @Test
    void aiTopicRejectsMissingTenant() {
        assertThatThrownBy(() -> DdcRedisKeys.aiTopic(null, "xingyuan", "dev", "order-service"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DdcRedisKeys.aiTopic(0L, "xingyuan", "dev", "order-service"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String hashTag(String key) {
        return key.substring(key.indexOf('{') + 1, key.indexOf('}'));
    }
}
