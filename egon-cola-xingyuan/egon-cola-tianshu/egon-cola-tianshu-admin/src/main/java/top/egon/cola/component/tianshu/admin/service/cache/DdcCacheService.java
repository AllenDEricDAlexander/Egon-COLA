package top.egon.cola.component.tianshu.admin.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.enums.ChangeType;
import top.egon.cola.component.tianshu.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service("ddcCacheService")
@RequiredArgsConstructor
public class DdcCacheService {

    @Qualifier("ddcConfigItemRepository") private final DdcConfigItemRepository configItemRepository;
    @Qualifier("ddcConfigVersionRepository") private final DdcConfigVersionRepository versionRepository;
    @Qualifier("ddcRedisRepository") private final DdcRedisRepository redisRepository;

    public int rebuild(String bizCode, String env, String appCode) {
        List<DdcConfigItemBO> items = configItemRepository.findByBizCodeAndEnvAndAppCode(bizCode, env, appCode);
        List<DdcConfigVersionBO> published = items.stream().map(this::publishedVersion)
                .flatMap(Optional::stream).filter(this::isRuntimeValue).toList();
        published.forEach(version -> redisRepository.writeConfig(bizCode, env, appCode,
                version.getResourceName(), version.getNewContent(), version.getConfigRevision()));
        return published.size();
    }

    public List<DdcCacheCheckRow> check(String bizCode, String env, String appCode) {
        return configItemRepository.findByBizCodeAndEnvAndAppCode(bizCode, env, appCode).stream()
                .map(this::publishedVersion).flatMap(Optional::stream).filter(this::isRuntimeValue)
                .map(version -> checkVersion(bizCode, env, appCode, version)).toList();
    }

    public DdcPageResult<DdcCacheCheckRow> page(String bizCode, String env, String appCode, PageQuery pageQuery) {
        DdcPageResult<DdcConfigVersionBO> page = versionRepository.findPublishedRuntimeVersions(
                bizCode, env, appCode, ChangeType.DELETE.name(), pageQuery);
        return DdcPageResult.<DdcCacheCheckRow>builder()
                .records(page.getRecords().stream().map(version -> checkVersion(bizCode, env, appCode, version)).toList())
                .total(page.getTotal()).pageNo(page.getPageNo()).pageSize(page.getPageSize()).build();
    }

    private DdcCacheCheckRow checkVersion(String bizCode, String env, String appCode, DdcConfigVersionBO version) {
        String redisValue = redisRepository.readConfigValue(bizCode, env, appCode, version.getResourceName());
        Long redisVersion = redisRepository.readConfigVersion(bizCode, env, appCode, version.getResourceName());
        boolean matched = Objects.equals(version.getNewContent(), redisValue)
                && Objects.equals(version.getConfigRevision(), redisVersion);
        return new DdcCacheCheckRow(version.getResourceName(), version.getNewContent(), redisValue,
                version.getConfigRevision(), redisVersion, matched);
    }

    private Optional<DdcConfigVersionBO> publishedVersion(DdcConfigItemBO item) {
        if (item.getPublishedVersion() == null) return Optional.empty();
        return Optional.of(versionRepository.findByConfigItemIdAndRevision(item.getId(), item.getPublishedVersion())
                .orElseThrow(() -> new DdcAdminException("published config version not found")));
    }

    private boolean isRuntimeValue(DdcConfigVersionBO version) {
        return !ChangeType.DELETE.name().equals(version.getChangeType());
    }
}
