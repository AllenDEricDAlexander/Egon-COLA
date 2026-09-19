package top.egon.cola.component.common.cache.core;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EgonColaTwoLevelCacheManagerTest {

    private EgonColaTwoLevelCacheManager manager(String nodeId) {
        EgonColaCacheProperties properties = new EgonColaCacheProperties();
        properties.setKeyPrefix("egon:cola:cache:it");
        properties.setNodeId(nodeId);
        return new EgonColaTwoLevelCacheManager(properties, mock(RedissonClient.class));
    }

    @Test
    void returnsSameHandleForSameRegion() {
        EgonColaTwoLevelCacheManager manager = manager("node-A");

        Cache first = manager.getCache("UserBO");
        Cache second = manager.getCache("UserBO");

        assertThat(first).isSameAs(second);
    }

    @Test
    void rejectsInvalidRegionName() {
        EgonColaTwoLevelCacheManager manager = manager("node-A");

        assertThatThrownBy(() -> manager.getCache("bad name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_NAME_INVALID");
        assertThatThrownBy(() -> manager.getCache(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_NAME_INVALID");
    }

    @Test
    void cacheNamesTrackTouchedRegions() {
        EgonColaTwoLevelCacheManager manager = manager("node-A");
        assertThat(manager.getCacheNames()).isEmpty();

        manager.getCache("UserBO");

        assertThat(manager.getCacheNames()).containsExactly("UserBO");
    }

    @Test
    void configuredNodeIdWinsAndGeneratedOneIsStable() {
        assertThat(manager("node-A").originNodeId()).isEqualTo("node-A");

        EgonColaTwoLevelCacheManager manager = manager("");
        assertThat(manager.originNodeId()).startsWith("node-");
        assertThat(manager.originNodeId()).isEqualTo(manager.originNodeId());
    }
}
