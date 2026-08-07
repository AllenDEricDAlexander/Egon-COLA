package top.egon.cola.component.ddc.admin.service.metadata;

import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcBizRepository;
import top.egon.cola.component.ddc.admin.repository.DdcEnvRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Gates registration and configuration pull on the enabled state of the whole
 * scope (biz, app, env, namespace). State is cached for five seconds so the
 * hot registration path does not query the database on every call; write
 * paths (disable/delete) must invalidate via {@link #invalidate(String)}.
 */
@Component
public class DdcScopeGate {

    private static final long CACHE_TTL_MILLIS = 5000;

    private final DdcBizRepository bizRepository;

    private final DdcAppRepository appRepository;

    private final DdcEnvRepository envRepository;

    private final DdcNamespaceRepository namespaceRepository;

    private final ConcurrentHashMap<String, Entry<?>> cache = new ConcurrentHashMap<>();

    public DdcScopeGate(DdcBizRepository bizRepository,
                        DdcAppRepository appRepository,
                        DdcEnvRepository envRepository,
                        DdcNamespaceRepository namespaceRepository) {
        this.bizRepository = bizRepository;
        this.appRepository = appRepository;
        this.envRepository = envRepository;
        this.namespaceRepository = namespaceRepository;
    }

    public void assertEnabled(String bizCode, String appCode, String env, String namespace) {
        assertPhysicalEnabled(bizCode, appCode, env);
        requireEnabled("namespace", bizCode + ":" + namespace,
                () -> namespaceRepository.findByBizCodeAndNamespaceCode(
                                bizCode, namespace)
                        .map(DdcNamespaceEntity::getEnabled));
    }

    public void assertPhysicalEnabled(String bizCode, String appCode, String env) {
        requireEnabled("biz", bizCode, () -> bizRepository.findByBizCode(bizCode)
                .map(DdcBizEntity::getEnabled));
        requireEnabled("app", bizCode + ":" + appCode,
                () -> appRepository.findByBizCodeAndAppCode(bizCode, appCode)
                .map(DdcAppEntity::getEnabled));
        requireEnabled("env", env, () -> envRepository.findByEnvCode(env)
                .map(DdcEnvEntity::getEnabled));
    }

    /**
     * Resolves the app's own biz code before checking the whole scope; used by
     * configuration pull requests that do not carry the biz dimension.
     */
    public void assertEnabledByApp(String appCode, String env, String namespace) {
        String bizCode = cachedValue("app-biz:" + appCode,
                () -> appRepository.findFirstByAppCodeOrderByBizCodeAsc(appCode)
                .map(DdcAppEntity::getBizCode)
                .orElse(null));
        assertEnabled(bizCode, appCode, env, namespace);
    }

    /** Called by disable/delete write paths so the next check re-reads state. */
    public void invalidate(String entityKey) {
        cache.remove(entityKey);
    }

    private void requireEnabled(String label, String code, Supplier<java.util.Optional<Boolean>> loader) {
        Boolean enabled = cached(label + ":" + code, loader);
        if (!Boolean.TRUE.equals(enabled)) {
            throw new CommonException(DdcErrorStatus.SCOPE_DISABLED);
        }
    }

    private Boolean cached(String key, Supplier<java.util.Optional<Boolean>> loader) {
        return cachedValue(key, () -> loader.get().orElse(null));
    }

    private <T> T cachedValue(String key, Supplier<T> loader) {
        long now = System.currentTimeMillis();
        Entry<?> entry = cache.get(key);
        if (entry != null && now - entry.loadedAtMillis() < CACHE_TTL_MILLIS) {
            @SuppressWarnings("unchecked")
            T value = (T) entry.value();
            return value;
        }
        T value = loader.get();
        cache.put(key, new Entry<>(value, now));
        return value;
    }

    private record Entry<T>(T value, long loadedAtMillis) {
    }
}
