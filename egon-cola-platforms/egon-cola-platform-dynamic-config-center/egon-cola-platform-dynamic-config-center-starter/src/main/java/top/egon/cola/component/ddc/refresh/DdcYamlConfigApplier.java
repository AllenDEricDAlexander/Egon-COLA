package top.egon.cola.component.ddc.refresh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.environment.DdcYamlPropertySourceLoader;
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

public class DdcYamlConfigApplier implements SmartInitializingSingleton {

    public static final String RESOURCE_NAME = "application.yml";

    public static final String PROPERTY_SOURCE_NAME = "ddc:" + RESOURCE_NAME;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcYamlConfigApplier.class);

    private final ConfigurableEnvironment environment;

    private final DdcConfigApplierRegistry applierRegistry;

    private final DdcFieldBindingService fieldBindingService;

    private final DdcConfigurationPropertiesRebinder rebinder;

    private final ApplicationEventPublisher eventPublisher;

    private final long maxYamlBytes;

    private final DdcYamlPropertySourceLoader yamlLoader =
            new DdcYamlPropertySourceLoader();

    private final DdcDynamicPropertySource propertySource;

    public DdcYamlConfigApplier(
            ConfigurableEnvironment environment,
            DdcConfigApplierRegistry applierRegistry,
            DdcFieldBindingService fieldBindingService,
            DdcConfigurationPropertiesRebinder rebinder,
            ApplicationEventPublisher eventPublisher,
            long maxYamlBytes) {
        this.environment = environment;
        this.applierRegistry = applierRegistry;
        this.fieldBindingService = fieldBindingService;
        this.rebinder = rebinder;
        this.eventPublisher = eventPublisher;
        this.maxYamlBytes = maxYamlBytes;
        this.propertySource = findPropertySource(environment);
    }

    public DdcDynamicPropertySource.Snapshot currentSnapshot() {
        return propertySource.snapshot();
    }

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
        try {
            refreshedKeys.addAll(rebinder.rebind(
                    effectiveDiff.changedKeys(),
                    effectiveDiff.removedKeys()
            ));
            applyLeaves(
                    effectiveDiff.changedKeys(),
                    previousResolved,
                    version,
                    refreshedKeys,
                    appliedLeaves
            );
        } catch (RuntimeException exception) {
            rollback(previous, effectiveDiff, appliedLeaves);
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

    private DdcDynamicPropertySource load(String content, long version) {
        try {
            return yamlLoader.load(RESOURCE_NAME, content, version);
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "DDC application.yml cannot be parsed",
                    exception
            );
        }
    }

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

    private Map<String, String> resolved(Set<String> keys) {
        Map<String, String> values = new LinkedHashMap<>();
        keys.forEach(key -> values.put(
                key,
                environment.getProperty(key)
        ));
        return values;
    }

    private void applyLeaves(Set<String> changedKeys,
                             Map<String, String> previousResolved,
                             long version,
                             Set<String> refreshedKeys,
                             List<AppliedLeaf> appliedLeaves) {
        changedKeys.stream()
                .filter(this::hasDynamicLeafConsumer)
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

    private boolean hasDynamicLeafConsumer(String key) {
        return applierRegistry.hasExplicitRegistration(key)
                || fieldBindingService.hasRefreshableBinding(key);
    }

    private void rollback(DdcDynamicPropertySource.Snapshot previous,
                          Diff diff,
                          List<AppliedLeaf> appliedLeaves) {
        propertySource.replace(previous);
        try {
            rebinder.rebind(diff.changedKeys(), Set.of());
        } catch (RuntimeException rollbackFailure) {
            LOGGER.warn("DDC configuration properties rollback failed", rollbackFailure);
        }
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
    }

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

    private record Diff(
            Set<String> changedKeys,
            Set<String> addedKeys,
            Set<String> updatedKeys,
            Set<String> removedKeys
    ) {
    }

    private record AppliedLeaf(
            String key,
            String previousValue,
            DdcConfigApplier applier
    ) {
    }
}
