package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcCacheServiceTest {

    @Test
    void rebuildWritesEnabledConfigsToRedis() {
        DdcConfigItemRepository configItemRepository = mock(DdcConfigItemRepository.class);
        DdcRedisRepository redisRepository = mock(DdcRedisRepository.class);
        DdcConfigItemEntity item = item("switch", "true", 2L);
        when(configItemRepository.findByAppCodeAndEnvAndNamespaceAndDeletedFalse("demo", "dev", "default"))
                .thenReturn(List.of(item));

        DdcCacheService service = new DdcCacheService(configItemRepository, redisRepository);
        int count = service.rebuild("demo", "dev", "default");

        assertThat(count).isEqualTo(1);
        verify(redisRepository).writeConfig("demo", "dev", "default", "switch", "true", 2L);
    }

    @Test
    void checkReportsMismatchedRedisValue() {
        DdcConfigItemRepository configItemRepository = mock(DdcConfigItemRepository.class);
        DdcRedisRepository redisRepository = mock(DdcRedisRepository.class);
        when(configItemRepository.findByAppCodeAndEnvAndNamespaceAndDeletedFalse("demo", "dev", "default"))
                .thenReturn(List.of(item("switch", "true", 2L)));
        when(redisRepository.readConfigValue("demo", "dev", "default", "switch")).thenReturn("false");
        when(redisRepository.readConfigVersion("demo", "dev", "default", "switch")).thenReturn(1L);

        DdcCacheService service = new DdcCacheService(configItemRepository, redisRepository);
        List<DdcCacheCheckRow> rows = service.check("demo", "dev", "default");

        assertThat(rows).singleElement().satisfies(row -> assertThat(row.isMatched()).isFalse());
    }

    private DdcConfigItemEntity item(String key, String value, Long version) {
        DdcConfigItemEntity item = new DdcConfigItemEntity();
        item.setConfigKey(key);
        item.setConfigValue(value);
        item.setCurrentVersion(version);
        item.setEnabled(true);
        item.setDeleted(false);
        return item;
    }
}
