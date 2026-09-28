package top.egon.cola.component.tianshu.admin.service.cache;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;

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
        DdcConfigItemBO item = item("switch", "draft", 2L, 1L);
        when(configItemRepository.findByBizCodeAndEnvAndAppCode("default", "dev", "demo"))
                .thenReturn(List.of(item));
        when(versionRepository.findByConfigItemIdAndRevision(11L, 1L))
                .thenReturn(java.util.Optional.of(version(11L, "switch", "true", 1L)));

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
        when(versionRepository.findByConfigItemIdAndRevision(11L, 1L))
                .thenReturn(java.util.Optional.of(version(11L, "switch", "true", 1L)));
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
        DdcConfigVersionBO current = version(12L, "application.yml", "true", 2L);
        when(versionRepository.findPublishedRuntimeVersions(
                eq("infra"), eq("prod"), eq("yuheng"), eq("DELETE"),
                any(PageQuery.class)))
                .thenReturn(DdcPageResult.<DdcConfigVersionBO>builder().records(List.of(current))
                        .total(2).pageNo(2).pageSize(1).build());
        when(redisRepository.readConfigValue(
                "infra", "prod", "yuheng", "application.yml"))
                .thenReturn("true");
        when(redisRepository.readConfigVersion(
                "infra", "prod", "yuheng", "application.yml"))
                .thenReturn(2L);
        DdcCacheService service = new DdcCacheService(
                configItemRepository, versionRepository, redisRepository);

        var page = service.page(
                "infra", "prod", "yuheng", new PageQuery(2, 1));

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).singleElement()
                .satisfies(row -> assertThat(row.isMatched()).isTrue());
        verify(redisRepository).readConfigValue(
                "infra", "prod", "yuheng", "application.yml");
        verify(redisRepository).readConfigVersion(
                "infra", "prod", "yuheng", "application.yml");
        verifyNoMoreInteractions(redisRepository);
    }

    private DdcConfigItemBO item(
            String key,
            String value,
            Long currentVersion,
            Long publishedVersion
    ) {
        return DdcConfigItemBO.builder().id(11L).publicId("config-" + key).resourceName(key)
                .content(value).currentVersion(currentVersion).publishedVersion(publishedVersion)
                .enabled(true).deleted(false).build();
    }

    private DdcConfigVersionBO version(Long configItemId, String key, String value, Long version) {
        return DdcConfigVersionBO.builder().configItemId(configItemId).resourceName(key)
                .newContent(value).configRevision(version).build();
    }
}
