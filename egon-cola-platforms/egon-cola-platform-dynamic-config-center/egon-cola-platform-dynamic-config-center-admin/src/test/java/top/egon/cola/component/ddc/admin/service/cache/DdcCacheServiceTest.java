package top.egon.cola.component.ddc.admin.service.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Test
    void pageChecksRedisOnlyForVersionsOnTheCurrentPage() {
        DdcConfigItemRepository configItemRepository = mock(DdcConfigItemRepository.class);
        DdcConfigVersionRepository versionRepository = mock(DdcConfigVersionRepository.class);
        DdcRedisRepository redisRepository = mock(DdcRedisRepository.class);
        DdcConfigVersionEntity current = version("application.yml", "true", 2L);
        when(versionRepository.findPublishedRuntimeVersions(
                eq("infra"), eq("prod"), eq("gateway"), eq("DELETE"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(current), PageRequest.of(1, 1), 2));
        when(redisRepository.readConfigValue(
                "infra", "prod", "gateway", "application.yml"))
                .thenReturn("true");
        when(redisRepository.readConfigVersion(
                "infra", "prod", "gateway", "application.yml"))
                .thenReturn(2L);
        DdcCacheService service = new DdcCacheService(
                configItemRepository, versionRepository, redisRepository);

        var page = service.page(
                "infra", "prod", "gateway", new PageQuery(2, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).singleElement()
                .satisfies(row -> assertThat(row.isMatched()).isTrue());
        verify(redisRepository).readConfigValue(
                "infra", "prod", "gateway", "application.yml");
        verify(redisRepository).readConfigVersion(
                "infra", "prod", "gateway", "application.yml");
        verifyNoMoreInteractions(redisRepository);
    }

    private DdcConfigItemEntity item(
            String key,
            String value,
            Long currentVersion,
            Long publishedVersion
    ) {
        DdcConfigItemEntity item = new DdcConfigItemEntity();
        item.setId("config-" + key);
        item.setResourceName(key);
        item.setContent(value);
        item.setCurrentVersion(currentVersion);
        item.setPublishedVersion(publishedVersion);
        item.setEnabled(true);
        item.setDeleted(false);
        return item;
    }

    private DdcConfigVersionEntity version(String key, String value, Long version) {
        DdcConfigVersionEntity item = new DdcConfigVersionEntity();
        item.setResourceName(key);
        item.setNewContent(value);
        item.setVersion(version);
        return item;
    }
}
