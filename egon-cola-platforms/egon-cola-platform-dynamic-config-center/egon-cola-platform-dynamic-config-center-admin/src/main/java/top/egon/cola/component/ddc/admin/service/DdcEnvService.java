package top.egon.cola.component.ddc.admin.service;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcEnvRepository;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DdcEnvService {

    private final DdcEnvRepository envRepository;

    private final DdcConfigItemRepository configItemRepository;

    private final ObjectProvider<RedissonClient> redissonProvider;

    public DdcEnvService(DdcEnvRepository envRepository,
                         DdcConfigItemRepository configItemRepository,
                         ObjectProvider<RedissonClient> redissonProvider) {
        this.envRepository = envRepository;
        this.configItemRepository = configItemRepository;
        this.redissonProvider = redissonProvider;
    }

    public List<DdcEnvEntity> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return envRepository.findAllByOrderBySortOrderAsc();
        }
        String trimmed = keyword.trim();
        return envRepository.findByEnvCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                trimmed, trimmed);
    }

    public DdcEnvEntity findByEnvCode(String envCode) {
        return require(envCode);
    }

    @Transactional
    public DdcEnvEntity save(DdcEnvEntity env) {
        LocalDateTime now = LocalDateTime.now();
        if (env.getId() == null) {
            env.setId(UuidV7.simpleString());
            env.setCreatedAt(now);
        }
        if (env.getEnabled() == null) {
            env.setEnabled(true);
        }
        if (env.getSortOrder() == null) {
            env.setSortOrder(0);
        }
        if (envRepository.existsByEnvCode(env.getEnvCode())) {
            throw new CommonException(DdcErrorStatus.ENV_CODE_EXISTS);
        }
        env.setUpdatedAt(now);
        return envRepository.save(env);
    }

    @Transactional
    public DdcEnvEntity update(String envCode, DdcEnvEntity request) {
        DdcEnvEntity existing = require(envCode);
        if (envRepository.existsByEnvCodeAndIdNot(request.getEnvCode(), existing.getId())) {
            throw new CommonException(DdcErrorStatus.ENV_CODE_EXISTS);
        }
        existing.setEnvCode(request.getEnvCode());
        existing.setDescription(request.getDescription());
        existing.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        existing.setUpdatedAt(LocalDateTime.now());
        return envRepository.save(existing);
    }

    @Transactional
    public void delete(String envCode) {
        DdcEnvEntity existing = require(envCode);
        if (configItemRepository.existsByEnv(envCode)) {
            throw new CommonException(DdcErrorStatus.ENV_IN_USE);
        }
        RedissonClient redisson = redissonProvider.getIfAvailable();
        if (redisson != null && redisson.getKeys()
                .getKeysByPattern("ddc:registry:catalog:*:*:" + envCode + ":*")
                .iterator().hasNext()) {
            throw new CommonException(DdcErrorStatus.ENV_IN_USE);
        }
        envRepository.delete(existing);
    }

    @Transactional
    public DdcEnvEntity setEnabled(String envCode, boolean enabled) {
        DdcEnvEntity existing = require(envCode);
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        return envRepository.save(existing);
    }

    private DdcEnvEntity require(String envCode) {
        return envRepository.findByEnvCode(envCode)
                .orElseThrow(() -> new CommonException(DdcErrorStatus.ENV_NOT_FOUND));
    }
}
