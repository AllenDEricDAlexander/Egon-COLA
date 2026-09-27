package top.egon.cola.component.tianshu.redis;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;

import static org.assertj.core.api.Assertions.assertThat;

class DdcTenantRedisKeyTest {

    @Test
    void sameScopeInDifferentTenantsUsesDifferentV4SlotsAndTopics() {
        String keyA = DdcRedisKeys.config(
                101L, "retail", "local", "order", "feature.enabled");
        String keyB = DdcRedisKeys.config(
                202L, "retail", "local", "order", "feature.enabled");
        String topicA = DdcRedisKeys.topic(101L, "retail", "local", "order");
        String topicB = DdcRedisKeys.topic(202L, "retail", "local", "order");

        assertThat(keyA).startsWith("tianshu:v4:tenant:101:{");
        assertThat(keyB).startsWith("tianshu:v4:tenant:202:{");
        assertThat(topicA).startsWith("tianshu:v4:tenant:101:{").endsWith(":topic");
        assertThat(topicB).startsWith("tianshu:v4:tenant:202:{").endsWith(":topic");
        assertThat(keyA).isNotEqualTo(keyB);
        assertThat(hashTag(keyA)).isNotEqualTo(hashTag(keyB));
        assertThat(topicA).isNotEqualTo(topicB);
    }

    @Test
    void tenantIsEncodedInRegistryAndGlobalCatalogSlots() {
        DdcServiceKey key = new DdcServiceKey(
                "retail", "local", "order", DdcServiceKind.HTTP_PROVIDER,
                "order-service", "default", "1.0.0", "http");
        String scopedA = DdcRedisKeys.registryInstance(101L, key, "instance-1");
        String scopedB = DdcRedisKeys.registryInstance(202L, key, "instance-1");
        String catalogA = DdcRedisKeys.globalRegistryCatalog(101L);
        String catalogB = DdcRedisKeys.globalRegistryCatalog(202L);

        assertThat(scopedA).startsWith("tianshu:v4:tenant:101:{");
        assertThat(scopedB).startsWith("tianshu:v4:tenant:202:{");
        assertThat(hashTag(scopedA)).isNotEqualTo(hashTag(scopedB));
        assertThat(catalogA).startsWith("tianshu:v4:tenant:101:{").endsWith(":services");
        assertThat(catalogB).startsWith("tianshu:v4:tenant:202:{").endsWith(":services");
        assertThat(hashTag(catalogA)).isNotEqualTo(hashTag(catalogB));
    }

    @Test
    void missingOrNonPositiveTenantIsRejectedWithoutV3Fallback() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                DdcRedisKeys.config(null, "retail", "local", "order", "key"))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                DdcRedisKeys.globalRegistryCatalog(0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(DdcRedisKeys.version(
                101L, "retail", "local", "order", "feature.enabled"))
                .startsWith("tianshu:v4:tenant:101:")
                .doesNotStartWith("tianshu:v3:");
    }

    private String hashTag(String key) {
        return key.substring(key.indexOf('{') + 1, key.indexOf('}'));
    }
}
