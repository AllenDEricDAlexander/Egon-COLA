package top.egon.cola.component.ddc.refresh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.format.DdcConfigFormatStrategyRegistry;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;
import top.egon.cola.component.ddc.model.enums.DdcConfigFormat;
import top.egon.cola.component.ddc.service.DdcConfigApplier;
import top.egon.cola.component.ddc.service.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 将动态下发的 YAML 配置事务式应用到 Spring 环境和运行时消费者。
 * Transactionally applies dynamically delivered YAML configuration to the Spring environment and runtime consumers.
 *
 * <p>本类先替换动态属性源，再刷新配置属性 Bean 和显式叶子应用器；任一步骤失败时恢复旧属性源并按逆序
 * 回滚已执行的叶子应用器。事件监听器失败只记录日志，不回滚已成功应用的配置。</p>
 *
 * <p>The dynamic property source is replaced before configuration-properties Beans and explicit leaf appliers are
 * refreshed. A failure restores the old source and rolls back completed leaf appliers in reverse order. Event-listener
 * failures are logged without rolling back successfully applied configuration.</p>
 */
public class DdcYamlConfigApplier implements SmartInitializingSingleton {

    /**
     * DDC YAML 资源的规范名称。 Canonical name of the DDC YAML resource.
     */
    public static final String RESOURCE_NAME =
            DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME;

    /**
     * Spring 环境中动态属性源的名称。 Name of the dynamic property source in the Spring environment.
     */
    public static final String PROPERTY_SOURCE_NAME = "ddc:" + RESOURCE_NAME;

    /**
     * 当前类的日志记录器。 Logger for this class.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcYamlConfigApplier.class);

    /**
     * 承载动态属性源并解析最终属性值的 Spring 环境。 Spring environment hosting the dynamic source and resolving final values.
     */
    private final ConfigurableEnvironment environment;

    /**
     * 解析显式配置键消费者的应用器注册表。 Registry resolving explicit configuration-key consumers.
     */
    private final DdcConfigApplierRegistry applierRegistry;

    /**
     * 管理字段级动态绑定的服务。 Service managing field-level dynamic bindings.
     */
    private final DdcFieldBindingService fieldBindingService;

    /**
     * 刷新配置属性 Bean 的重新绑定器。 Rebinder that refreshes configuration-properties Beans.
     */
    private final DdcConfigurationPropertiesRebinder rebinder;

    /**
     * 发布配置变化事件的 Spring 发布器。 Spring publisher for configuration-change events.
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 允许的 YAML UTF-8 字节数上限。 Maximum allowed YAML size in UTF-8 bytes.
     */
    private final long maxYamlBytes;

    /**
     * 选择 YAML 解析实现的配置格式策略注册表。
     * Configuration-format strategy registry that selects the YAML parser implementation.
     */
    private final DdcConfigFormatStrategyRegistry formatStrategies;

    /**
     * 当前环境中的可替换 DDC 动态属性源。 Replaceable DDC dynamic property source in the current environment.
     */
    private final DdcDynamicPropertySource propertySource;

    /**
     * 创建 YAML 配置应用器并定位已由 ConfigData 安装的动态属性源。
     * Creates the YAML configuration applier and locates the dynamic source installed by ConfigData.
     *
     * @param environment         可配置 Spring 环境; configurable Spring environment
     * @param applierRegistry     配置应用器注册表; configuration applier registry
     * @param fieldBindingService 字段绑定服务; field binding service
     * @param rebinder            配置属性重新绑定器; configuration-properties rebinder
     * @param eventPublisher      应用事件发布器; application event publisher
     * @param maxYamlBytes        YAML UTF-8 字节数上限; maximum YAML size in UTF-8 bytes
     * @param formatStrategies    配置格式策略注册表; configuration-format strategy registry
     * @throws IllegalStateException 环境中缺少 DDC ConfigData 属性源时抛出; thrown when the DDC ConfigData source is absent
     */
    public DdcYamlConfigApplier(
            ConfigurableEnvironment environment,
            DdcConfigApplierRegistry applierRegistry,
            DdcFieldBindingService fieldBindingService,
            DdcConfigurationPropertiesRebinder rebinder,
            ApplicationEventPublisher eventPublisher,
            long maxYamlBytes,
            DdcConfigFormatStrategyRegistry formatStrategies) {
        this.environment = environment;
        this.applierRegistry = applierRegistry;
        this.fieldBindingService = fieldBindingService;
        this.rebinder = rebinder;
        this.eventPublisher = eventPublisher;
        this.maxYamlBytes = maxYamlBytes;
        this.formatStrategies = Objects.requireNonNull(
                formatStrategies,
                "formatStrategies"
        );
        this.propertySource = findPropertySource(environment);
    }

    /**
     * 返回当前动态配置快照。
     * Returns the current dynamic configuration snapshot.
     *
     * @return 当前不可变快照; current immutable snapshot
     */
    public DdcDynamicPropertySource.Snapshot currentSnapshot() {
        return propertySource.snapshot();
    }

    /**
     * 在单例初始化完成后，将启动期加载的值按优先级应用到显式注册的运行时消费者。
     * Applies startup-loaded values to explicitly registered runtime consumers in priority order after singleton initialization.
     */
    @Override
    public void afterSingletonsInstantiated() {
        DdcDynamicPropertySource.Snapshot snapshot =
                propertySource.snapshot();
        snapshot.values().keySet().stream()
                .filter(applierRegistry::hasExplicitRegistration)
                .sorted(Comparator
                        .comparingInt((String key) ->
                                applierRegistry.resolve(key).priority())
                        .thenComparing(String::compareTo))
                .forEach(key -> applierRegistry.resolve(key).apply(
                        key,
                        environment.getProperty(key),
                        snapshot.version()
                ));
    }

    /**
     * 解析并应用一个新 YAML 版本，失败时恢复此前的动态状态。
     * Parses and applies a new YAML version, restoring the previous dynamic state on failure.
     *
     * @param content  YAML 文本内容; YAML text content
     * @param version  配置版本; configuration version
     * @param changeId 发布变化标识; publication change identifier
     * @return 描述有效变化与刷新结果的事件; event describing effective changes and refresh results
     * @throws IllegalArgumentException 内容超限或无法解析时抛出; thrown when content exceeds the limit or cannot be parsed
     * @throws RuntimeException         运行时消费者应用失败时抛出; thrown when a runtime consumer fails to apply the change
     */
    public DdcConfigurationChangedEvent apply(
            String content,
            long version,
            String changeId) {
        validateSize(content);
        DdcDynamicPropertySource candidate = load(content, version);
        DdcDynamicPropertySource.Snapshot previous =
                propertySource.snapshot();
        Diff rawDiff = diff(previous, candidate.snapshot());
        Map<String, String> previousResolved = resolved(rawDiff.changedKeys());

        propertySource.replace(candidate.snapshot());
        Diff effectiveDiff = effectiveDiff(rawDiff, previousResolved);
        Set<String> refreshedKeys = new LinkedHashSet<>();
        List<AppliedLeaf> appliedLeaves = new ArrayList<>();
        DdcFieldBindingService.RefreshResult fieldRefresh = null;
        try {
            refreshedKeys.addAll(rebinder.rebind(
                    effectiveDiff.changedKeys(),
                    effectiveDiff.removedKeys()
            ));
            fieldRefresh = fieldBindingService.refresh();
            applyLeaves(
                    effectiveDiff.changedKeys(),
                    previousResolved,
                    version,
                    refreshedKeys,
                    appliedLeaves
            );
        } catch (RuntimeException exception) {
            rollback(
                    previous,
                    effectiveDiff,
                    fieldRefresh,
                    appliedLeaves
            );
            throw exception;
        }

        Set<String> restartRequiredKeys =
                new LinkedHashSet<>(effectiveDiff.changedKeys());
        restartRequiredKeys.removeAll(refreshedKeys);
        DdcConfigurationChangedEvent event =
                new DdcConfigurationChangedEvent(
                        RESOURCE_NAME,
                        version,
                        candidate.snapshot().checksum(),
                        effectiveDiff.changedKeys(),
                        effectiveDiff.addedKeys(),
                        effectiveDiff.updatedKeys(),
                        effectiveDiff.removedKeys(),
                        refreshedKeys,
                        restartRequiredKeys,
                        changeId
                );
        publish(event);
        return event;
    }

    /**
     * 将 YAML 文本加载为候选动态属性源。
     * Loads YAML text into a candidate dynamic property source.
     *
     * @param content YAML 文本内容; YAML text content
     * @param version 配置版本; configuration version
     * @return 候选动态属性源; candidate dynamic property source
     * @throws IllegalArgumentException YAML 无法解析时抛出; thrown when the YAML cannot be parsed
     */
    private DdcDynamicPropertySource load(String content, long version) {
        try {
            return formatStrategies.get(DdcConfigFormat.YAML).load(
                    RESOURCE_NAME,
                    content,
                    version
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "DDC application.yml cannot be parsed",
                    exception
            );
        }
    }

    /**
     * 校验 YAML 内容的 UTF-8 字节数未超过配置上限。
     * Validates that the YAML content does not exceed the configured UTF-8 byte limit.
     *
     * @param content YAML 文本内容，可为 {@code null}; YAML text content, possibly {@code null}
     * @throws IllegalArgumentException 内容超限时抛出; thrown when the content exceeds the limit
     */
    private void validateSize(String content) {
        int size = content == null
                ? 0
                : content.getBytes(StandardCharsets.UTF_8).length;
        if (size > maxYamlBytes) {
            throw new IllegalArgumentException(
                    "DDC application.yml exceeds the UTF-8 limit of "
                            + maxYamlBytes + " bytes"
            );
        }
    }

    /**
     * 按属性源原始值计算新增、更新和删除差异。
     * Computes added, updated, and removed differences using raw property-source values.
     *
     * @param previous  旧快照; previous snapshot
     * @param candidate 候选快照; candidate snapshot
     * @return 原始差异; raw difference
     */
    private Diff diff(DdcDynamicPropertySource.Snapshot previous,
                      DdcDynamicPropertySource.Snapshot candidate) {
        Map<String, Object> oldValues = previous.values();
        Map<String, Object> newValues = candidate.values();
        Set<String> keys = new LinkedHashSet<>(oldValues.keySet());
        keys.addAll(newValues.keySet());
        Set<String> changed = new LinkedHashSet<>();
        Set<String> added = new LinkedHashSet<>();
        Set<String> updated = new LinkedHashSet<>();
        Set<String> removed = new LinkedHashSet<>();
        for (String key : keys) {
            boolean oldPresent = oldValues.containsKey(key);
            boolean newPresent = newValues.containsKey(key);
            if (oldPresent && newPresent
                    && Objects.equals(oldValues.get(key), newValues.get(key))) {
                continue;
            }
            changed.add(key);
            if (!oldPresent) {
                added.add(key);
            } else if (!newPresent) {
                removed.add(key);
            } else {
                updated.add(key);
            }
        }
        return new Diff(changed, added, updated, removed);
    }

    /**
     * 移除因更高优先级属性源覆盖而未改变最终解析值的原始差异。
     * Removes raw differences whose final resolved values remain unchanged because of higher-priority property sources.
     *
     * @param rawDiff          原始属性源差异; raw property-source difference
     * @param previousResolved 变化前的最终解析值; final resolved values before replacement
     * @return 对运行时实际可见的有效差异; effective difference visible to runtime consumers
     */
    private Diff effectiveDiff(Diff rawDiff,
                               Map<String, String> previousResolved) {
        Set<String> changed = new LinkedHashSet<>();
        Set<String> added = new LinkedHashSet<>();
        Set<String> updated = new LinkedHashSet<>();
        Set<String> removed = new LinkedHashSet<>();
        for (String key : rawDiff.changedKeys()) {
            if (Objects.equals(
                    previousResolved.get(key),
                    environment.getProperty(key)
            )) {
                continue;
            }
            changed.add(key);
            if (rawDiff.addedKeys().contains(key)) {
                added.add(key);
            } else if (rawDiff.removedKeys().contains(key)) {
                removed.add(key);
            } else {
                updated.add(key);
            }
        }
        return new Diff(changed, added, updated, removed);
    }

    /**
     * 读取指定键在当前 Spring 环境中的最终解析值。
     * Reads final resolved values for the specified keys from the current Spring environment.
     *
     * @param keys 配置键集合; configuration keys
     * @return 保持键迭代顺序的解析值映射; resolved value map preserving key iteration order
     */
    private Map<String, String> resolved(Set<String> keys) {
        Map<String, String> values = new LinkedHashMap<>();
        keys.forEach(key -> values.put(
                key,
                environment.getProperty(key)
        ));
        return values;
    }

    /**
     * 按应用器优先级和配置键顺序应用具有动态消费者的叶子配置。
     * Applies leaf configuration having dynamic consumers in applier-priority and key order.
     *
     * @param changedKeys      有效变化键; effectively changed keys
     * @param previousResolved 变化前解析值; resolved values before the change
     * @param version          配置版本; configuration version
     * @param refreshedKeys    已完成运行时刷新的键集合; keys already refreshed at runtime
     * @param appliedLeaves    成功应用并用于失败回滚的记录; successful applications recorded for rollback
     */
    private void applyLeaves(Set<String> changedKeys,
                             Map<String, String> previousResolved,
                             long version,
                             Set<String> refreshedKeys,
                             List<AppliedLeaf> appliedLeaves) {
        changedKeys.stream()
                .filter(applierRegistry::hasExplicitRegistration)
                .sorted(Comparator
                        .comparingInt((String key) ->
                                applierRegistry.resolve(key).priority())
                        .thenComparing(String::compareTo))
                .forEach(key -> {
                    DdcConfigApplier applier =
                            applierRegistry.resolve(key);
                    applier.apply(
                            key,
                            environment.getProperty(key),
                            version
                    );
                    appliedLeaves.add(new AppliedLeaf(
                            key,
                            previousResolved.get(key),
                            applier
                    ));
                    refreshedKeys.add(key);
                });
    }

    /**
     * 恢复旧属性源，并按逆序尽力回滚叶子应用器、字段和配置属性 Bean。
     * Restores the previous source and best-effort rolls back leaf appliers, fields, and configuration-properties beans in reverse order.
     *
     * @param previous      旧动态配置快照; previous dynamic configuration snapshot
     * @param diff          需要回滚的有效差异; effective difference to roll back
     * @param fieldRefresh  已完成字段刷新，字段刷新前失败时为 {@code null}; completed field refresh, or {@code null} when field refresh failed before completion
     * @param appliedLeaves 已成功应用的叶子记录; successfully applied leaf records
     */
    private void rollback(DdcDynamicPropertySource.Snapshot previous,
                          Diff diff,
                          DdcFieldBindingService.RefreshResult fieldRefresh,
                          List<AppliedLeaf> appliedLeaves) {
        propertySource.replace(previous);
        for (int index = appliedLeaves.size() - 1; index >= 0; index--) {
            AppliedLeaf applied = appliedLeaves.get(index);
            try {
                applied.applier().apply(
                        applied.key(),
                        applied.previousValue(),
                        previous.version()
                );
            } catch (RuntimeException rollbackFailure) {
                LOGGER.warn(
                        "DDC config leaf rollback failed for key={}",
                        applied.key(),
                        rollbackFailure
                );
            }
        }
        if (fieldRefresh != null) {
            fieldBindingService.rollback(fieldRefresh);
        }
        try {
            rebinder.rebind(diff.changedKeys(), Set.of());
        } catch (RuntimeException rollbackFailure) {
            LOGGER.warn("DDC configuration properties rollback failed", rollbackFailure);
        }
    }

    /**
     * 发布配置变化事件，隔离并记录监听器异常。
     * Publishes a configuration-change event while isolating and logging listener failures.
     *
     * @param event 配置变化事件; configuration-change event
     */
    private void publish(DdcConfigurationChangedEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "DDC configuration change listener failed for version={}",
                    event.version(),
                    exception
            );
        }
    }

    /**
     * 在环境属性源树中定位规范名称的 DDC 动态属性源。
     * Locates the canonically named DDC dynamic property source in the environment property-source tree.
     *
     * @param environment 可配置 Spring 环境; configurable Spring environment
     * @return DDC 动态属性源; DDC dynamic property source
     * @throws IllegalStateException 未导入 DDC ConfigData 资源时抛出; thrown when the DDC ConfigData resource was not imported
     */
    private DdcDynamicPropertySource findPropertySource(
            ConfigurableEnvironment environment) {
        for (PropertySource<?> source : environment.getPropertySources()) {
            DdcDynamicPropertySource found = findPropertySource(source);
            if (found != null) {
                return found;
            }
        }
        throw new IllegalStateException(
                "DDC ConfigData source is missing; add "
                        + "spring.config.import=ddc:application.yml"
        );
    }

    /**
     * 递归搜索单个属性源及其组合子源。
     * Recursively searches one property source and any nested composite sources.
     *
     * @param source 待搜索的属性源; property source to search
     * @return 匹配的动态属性源，未找到时为 {@code null}; matching dynamic source, or {@code null} when absent
     */
    private DdcDynamicPropertySource findPropertySource(
            PropertySource<?> source) {
        if (source instanceof DdcDynamicPropertySource dynamic
                && PROPERTY_SOURCE_NAME.equals(dynamic.getName())) {
            return dynamic;
        }
        if (source instanceof CompositePropertySource composite) {
            for (PropertySource<?> nested
                    : composite.getPropertySources()) {
                DdcDynamicPropertySource found = findPropertySource(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 表示一次配置快照的分类差异。
     * Categorized difference between configuration snapshots.
     *
     * @param changedKeys 所有变化键; all changed keys
     * @param addedKeys   新增键; added keys
     * @param updatedKeys 更新键; updated keys
     * @param removedKeys 删除键; removed keys
     */
    private record Diff(
            Set<String> changedKeys,
            Set<String> addedKeys,
            Set<String> updatedKeys,
            Set<String> removedKeys
    ) {
    }

    /**
     * 保存已应用叶子的回滚信息。
     * Stores rollback information for an applied leaf.
     *
     * @param key           配置键; configuration key
     * @param previousValue 变化前最终解析值; final resolved value before the change
     * @param applier       执行应用和回滚的应用器; applier used for application and rollback
     */
    private record AppliedLeaf(
            String key,
            String previousValue,
            DdcConfigApplier applier
    ) {
    }
}
