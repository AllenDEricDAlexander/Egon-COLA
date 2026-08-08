package top.egon.cola.component.ddc.admin.service.cache;

import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.enums.ChangeType;
import top.egon.cola.component.ddc.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DdcCacheService {

    private final DdcConfigItemRepository configItemRepository;

    private final DdcConfigVersionRepository versionRepository;

    private final DdcRedisRepository redisRepository;

    public DdcCacheService(
            DdcConfigItemRepository configItemRepository,
            DdcConfigVersionRepository versionRepository,
            DdcRedisRepository redisRepository
    ) {
        this.configItemRepository = configItemRepository;
        this.versionRepository = versionRepository;
        this.redisRepository = redisRepository;
    }

    public int rebuild(String bizCode, String env, String appCode) {
        List<DdcConfigItemEntity> items = configItemRepository
                .findByBizCodeAndEnvAndAppCode(bizCode, env, appCode);
        List<DdcConfigVersionEntity> published = items.stream()
                .map(this::publishedVersion)
                .flatMap(Optional::stream)
                .filter(this::isRuntimeValue)
                .toList();
        published.forEach(version -> redisRepository.writeConfig(
                bizCode,
                env,
                appCode,
                version.getResourceName(),
                version.getNewContent(),
                version.getVersion()
        ));
        return published.size();
    }

    public List<DdcCacheCheckRow> check(
            String bizCode, String env, String appCode) {
        return configItemRepository.findByBizCodeAndEnvAndAppCode(
                        bizCode, env, appCode
                ).stream()
                .map(this::publishedVersion)
                .flatMap(Optional::stream)
                .filter(this::isRuntimeValue)
                .map(version -> checkVersion(bizCode, env, appCode, version))
                .toList();
    }

    private DdcCacheCheckRow checkVersion(
            String bizCode,
            String env,
            String appCode,
            DdcConfigVersionEntity version
    ) {
        String redisValue = redisRepository.readConfigValue(
                bizCode, env, appCode, version.getResourceName()
        );
        Long redisVersion = redisRepository.readConfigVersion(
                bizCode, env, appCode, version.getResourceName()
        );
        boolean matched = Objects.equals(version.getNewContent(), redisValue)
                && Objects.equals(version.getVersion(), redisVersion);
        return new DdcCacheCheckRow(
                version.getResourceName(),
                version.getNewContent(),
                redisValue,
                version.getVersion(),
                redisVersion,
                matched
        );
    }

    private Optional<DdcConfigVersionEntity> publishedVersion(
            DdcConfigItemEntity item
    ) {
        if (item.getPublishedVersion() == null) {
            return Optional.empty();
        }
        return Optional.of(versionRepository.findByConfigIdAndVersion(
                item.getId(),
                item.getPublishedVersion()
        ).orElseThrow(() -> new DdcAdminException(
                "published config version not found"
        )));
    }

    private boolean isRuntimeValue(DdcConfigVersionEntity version) {
        return !ChangeType.DELETE.name().equals(version.getChangeType());
    }
}
