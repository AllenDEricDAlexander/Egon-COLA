package top.egon.cola.component.ddc.admin.service;

import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;

import java.util.List;
import java.util.Objects;

@Service
public class DdcCacheService {

    private final DdcConfigItemRepository configItemRepository;

    private final DdcRedisRepository redisRepository;

    public DdcCacheService(DdcConfigItemRepository configItemRepository, DdcRedisRepository redisRepository) {
        this.configItemRepository = configItemRepository;
        this.redisRepository = redisRepository;
    }

    public int rebuild(String appCode, String env, String namespace) {
        List<DdcConfigItemEntity> items = configItemRepository.findByAppCodeAndEnvAndNamespaceAndDeletedFalse(appCode, env, namespace);
        items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .forEach(item -> redisRepository.writeConfig(appCode, env, namespace, item.getConfigKey(),
                        item.getConfigValue(), item.getCurrentVersion()));
        return items.size();
    }

    public List<DdcCacheCheckRow> check(String appCode, String env, String namespace) {
        return configItemRepository.findByAppCodeAndEnvAndNamespaceAndDeletedFalse(appCode, env, namespace).stream()
                .map(item -> checkItem(appCode, env, namespace, item))
                .toList();
    }

    private DdcCacheCheckRow checkItem(String appCode, String env, String namespace, DdcConfigItemEntity item) {
        String redisValue = redisRepository.readConfigValue(appCode, env, namespace, item.getConfigKey());
        Long redisVersion = redisRepository.readConfigVersion(appCode, env, namespace, item.getConfigKey());
        boolean matched = Objects.equals(item.getConfigValue(), redisValue)
                && Objects.equals(item.getCurrentVersion(), redisVersion);
        return new DdcCacheCheckRow(item.getConfigKey(), item.getConfigValue(), redisValue,
                item.getCurrentVersion(), redisVersion, matched);
    }
}
