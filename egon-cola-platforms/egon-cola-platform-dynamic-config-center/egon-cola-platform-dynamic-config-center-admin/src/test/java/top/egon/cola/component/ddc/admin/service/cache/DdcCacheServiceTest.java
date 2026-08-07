package top.egon.cola.component.ddc.admin.service.cache;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DdcCacheServiceTest {

    @Test
    void rebuildWritesEnabledConfigsToRedis() {
        DdcConfigItemRepository configItemRepository = mock(DdcConfigItemRepository.class);
        DdcConfigVersionRepository versionRepository = mock(DdcConfigVersionRepository.class);
        DdcRedisRepository redisRepository = mock(DdcRedisRepository.class);
        DdcConfigItemEntity item = item("switch", "draft", 2L, 1L);
        when(configItemRepository.findByBizCodeAndEnvAndAppCode("default", "dev", "demo"))
                .thenReturn(List.of(item));
        when(versionRepository.findByConfigIdAndVersion("config-switch", 1L))
                .thenReturn(java.util.Optional.of(version("switch", "true", 1L)));

        DdcCacheService service = new DdcCacheService(
                configItemRepository, versionRepository, redisRepository
        );
        int count = service.rebuild("default", "dev", "demo");

        assertThat(count).isEqualTo(1);
        verify(redisRepository).writeConfig("default", "dev", "demo", "switch", "true", 1L);
    }

    @Test
    void checkReportsMismatchedRedisValue() {
        DdcConfigItemRepository configItemRepository = mock(DdcConfigItemRepository.class);
        DdcConfigVersionRepository versionRepository = mock(DdcConfigVersionRepository.class);
        DdcRedisRepository redisRepository = mock(DdcRedisRepository.class);
        when(configItemRepository.findByBizCodeAndEnvAndAppCode("default", "dev", "demo"))
                .thenReturn(List.of(item("switch", "draft", 2L, 1L)));
        when(versionRepository.findByConfigIdAndVersion("config-switch", 1L))
                .thenReturn(java.util.Optional.of(version("switch", "true", 1L)));
        when(redisRepository.readConfigValue("default", "dev", "demo", "switch")).thenReturn("false");
        when(redisRepository.readConfigVersion("default", "dev", "demo", "switch")).thenReturn(1L);

        DdcCacheService service = new DdcCacheService(
                configItemRepository, versionRepository, redisRepository
        );
        List<DdcCacheCheckRow> rows = service.check("default", "dev", "demo");

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getDatabaseValue()).isEqualTo("true");
            assertThat(row.getDatabaseVersion()).isEqualTo(1L);
            assertThat(row.isMatched()).isFalse();
        });
    }

    @Test
    void rebuildSkipsDraftWithoutPublishedPointer() {
        DdcConfigItemRepository configItemRepository = mock(DdcConfigItemRepository.class);
        DdcConfigVersionRepository versionRepository = mock(DdcConfigVersionRepository.class);
        DdcRedisRepository redisRepository = mock(DdcRedisRepository.class);
        when(configItemRepository.findByBizCodeAndEnvAndAppCode(
                "default", "dev", "demo"
        )).thenReturn(List.of(item("switch", "draft", 1L, null)));
        DdcCacheService service = new DdcCacheService(
                configItemRepository, versionRepository, redisRepository
        );

        assertThat(service.rebuild("default", "dev", "demo")).isZero();
        verifyNoInteractions(versionRepository, redisRepository);
    }

    private DdcConfigItemEntity item(
            String key,
            String value,
            Long currentVersion,
            Long publishedVersion
    ) {
        DdcConfigItemEntity item = new DdcConfigItemEntity();
        item.setId("config-" + key);
        item.setConfigKey(key);
        item.setConfigValue(value);
        item.setCurrentVersion(currentVersion);
        item.setPublishedVersion(publishedVersion);
        item.setEnabled(true);
        item.setDeleted(false);
        return item;
    }

    private DdcConfigVersionEntity version(String key, String value, Long version) {
        DdcConfigVersionEntity item = new DdcConfigVersionEntity();
        item.setConfigKey(key);
        item.setNewValue(value);
        item.setVersion(version);
        return item;
    }
}
