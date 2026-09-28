package top.egon.cola.component.tianshu.admin.service.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcEnvBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcBizRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcEnvRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Gates registration and configuration pull on the enabled state of the whole
 * scope (biz, app, env, namespace). State is cached for five seconds so the
 * hot registration path does not query the database on every call; write
 * paths (disable/delete) must invalidate via {@link #invalidate(String)}.
 */
@Slf4j
@RequiredArgsConstructor
@Component("ddcScopeGate")
public class DdcScopeGate {

    private static final Duration CACHE_TTL = Duration.ofSeconds(5);

    @Qualifier("ddcBizRepository")
    private final DdcBizRepository bizRepository;

    @Qualifier("ddcAppRepository")
    private final DdcAppRepository appRepository;

    @Qualifier("ddcEnvRepository")
    private final DdcEnvRepository envRepository;

    @Qualifier("ddcNamespaceRepository")
    private final DdcNamespaceRepository namespaceRepository;

    private final ConcurrentHashMap<String, Entry<?>> cache = new ConcurrentHashMap<>();

    public void assertEnabled(String bizCode, String appCode, String env, String namespace) {
        assertPhysicalEnabled(bizCode, appCode, env);
        requireEnabled("namespace", bizCode + ":" + namespace,
                () -> namespaceRepository.findByBizCodeAndNamespaceCode(
                                bizCode, namespace)
                        .map(DdcNamespaceBO::getEnabled));
    }

    public void assertPhysicalEnabled(String bizCode, String appCode, String env) {
        requireEnabled("biz", bizCode, () -> bizRepository.findByBizCode(bizCode)
                .map(DdcBizBO::getEnabled));
        requireEnabled("app", bizCode + ":" + appCode,
                () -> appRepository.findByBizCodeAndAppCode(bizCode, appCode)
                .map(DdcAppBO::getEnabled));
        requireEnabled("env", env, () -> envRepository.findByEnvCode(env)
                .map(DdcEnvBO::getEnabled));
    }

    /**
     * Resolves the app's own biz code before checking the whole scope; used by
     * configuration pull requests that do not carry the biz dimension.
     */
    public void assertEnabledByApp(String appCode, String env, String namespace) {
        String bizCode = cachedValue("app-biz:" + appCode,
                () -> appRepository.findFirstByAppCodeOrderByBizCodeAsc(appCode)
                .map(DdcAppBO::getBizCode)
                .orElse(null));
        assertEnabled(bizCode, appCode, env, namespace);
    }

    /** Called by disable/delete write paths so the next check re-reads state. */
    public void invalidate(String entityKey) {
        cache.remove(cacheKey(entityKey));
    }

    private void requireEnabled(String label, String code, Supplier<java.util.Optional<Boolean>> loader) {
        Boolean enabled = cached(label + ":" + code, loader);
        if (!Boolean.TRUE.equals(enabled)) {
            log.debug("Denied disabled Tianshu scope for tenantId={}, dimension={}",
                    tenantId(), label);
            throw new CommonException(DdcErrorStatus.SCOPE_DISABLED);
        }
    }

    private Boolean cached(String key, Supplier<java.util.Optional<Boolean>> loader) {
        return cachedValue(key, () -> loader.get().orElse(null));
    }

    private <T> T cachedValue(String key, Supplier<T> loader) {
        Instant now = Instant.now();
        String tenantKey = cacheKey(key);
        Entry<?> entry = cache.get(tenantKey);
        if (entry != null && Duration.between(entry.loadedAt(), now).compareTo(CACHE_TTL) < 0) {
            @SuppressWarnings("unchecked")
            T value = (T) entry.value();
            return value;
        }
        T value = loader.get();
        cache.put(tenantKey, new Entry<>(value, now));
        return value;
    }

    private String cacheKey(String key) {
        return tenantId() + ":" + key;
    }

    private long tenantId() {
        String value = MDC.get("tenantId");
        try {
            long tenantId = Long.parseLong(value);
            if (tenantId > 0 && Long.toString(tenantId).equals(value)) {
                return tenantId;
            }
        } catch (NumberFormatException ignored) {
            // A request without trusted tenant MDC is rejected below.
        }
        throw new IllegalStateException("A positive trusted tenantId is required for Tianshu scope checks");
    }

    private record Entry<T>(T value, Instant loadedAt) {
    }
}
