package top.egon.cola.component.ddc.format;

import top.egon.cola.component.ddc.model.enums.DdcConfigFormat;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 按配置格式保存并选择解析策略的不可变注册表。
 * Immutable registry that stores and selects parsing strategies by configuration format.
 *
 * <p>默认注册表只包含 YAML 策略。它同时供 ConfigData、运行时刷新和管理端校验使用，保证三条链路采用
 * 同一套格式识别与解析规则。</p>
 *
 * <p>The default registry contains only the YAML strategy. ConfigData, runtime refresh, and administration
 * validation all use it so the three paths share identical format detection and parsing rules.</p>
 */
public final class DdcConfigFormatStrategyRegistry {

    /**
     * 当前版本共享的 YAML-only 默认注册表。 Shared YAML-only default registry for the current version.
     */
    private static final DdcConfigFormatStrategyRegistry DEFAULTS =
            new DdcConfigFormatStrategyRegistry(List.of(
                    new DdcYamlConfigFormatStrategy()
            ));

    /**
     * 按配置格式索引的不可变策略映射。 Immutable strategy map indexed by configuration format.
     */
    private final Map<DdcConfigFormat, DdcConfigFormatStrategy> strategies;

    /**
     * 使用指定策略集合创建注册表，并拒绝重复格式。
     * Creates a registry from the specified strategies and rejects duplicate formats.
     *
     * @param strategies 配置格式策略集合; configuration-format strategies
     * @throws NullPointerException     策略集合或其中元素为 {@code null} 时抛出; thrown when the collection or an element is {@code null}
     * @throws IllegalArgumentException 同一格式登记多个策略时抛出; thrown when multiple strategies claim the same format
     */
    public DdcConfigFormatStrategyRegistry(
            Collection<? extends DdcConfigFormatStrategy> strategies) {
        Objects.requireNonNull(strategies, "strategies");
        EnumMap<DdcConfigFormat, DdcConfigFormatStrategy> indexed =
                new EnumMap<>(DdcConfigFormat.class);
        for (DdcConfigFormatStrategy strategy : strategies) {
            Objects.requireNonNull(strategy, "strategy");
            DdcConfigFormat format = Objects.requireNonNull(
                    strategy.format(),
                    "strategy format"
            );
            if (indexed.putIfAbsent(format, strategy) != null) {
                throw new IllegalArgumentException(
                        "Duplicate DDC config format strategy: " + format
                );
            }
        }
        this.strategies = Map.copyOf(indexed);
    }

    /**
     * 返回共享的 YAML-only 默认注册表。
     * Returns the shared YAML-only default registry.
     *
     * @return 默认策略注册表; default strategy registry
     */
    public static DdcConfigFormatStrategyRegistry defaults() {
        return DEFAULTS;
    }

    /**
     * 按枚举格式取得策略。
     * Obtains a strategy by enum format.
     *
     * @param format 配置格式; configuration format
     * @return 对应策略; corresponding strategy
     * @throws IllegalArgumentException 当前注册表未登记该格式时抛出; thrown when the format is not registered
     */
    public DdcConfigFormatStrategy get(DdcConfigFormat format) {
        DdcConfigFormatStrategy strategy = strategies.get(
                Objects.requireNonNull(format, "format")
        );
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported DDC config format: " + format
            );
        }
        return strategy;
    }

    /**
     * 按外部格式名称取得策略。
     * Obtains a strategy by external format name.
     *
     * @param format 外部格式名称; external format name
     * @return 对应策略; corresponding strategy
     * @throws IllegalArgumentException 格式名称无效或未登记时抛出; thrown when the format name is invalid or unregistered
     */
    public DdcConfigFormatStrategy get(String format) {
        return get(DdcConfigFormat.from(format));
    }

    /**
     * 按外部格式名称和资源名取得策略，并校验两者匹配。
     * Obtains a strategy by external format name and resource name, validating that they match.
     *
     * @param format       外部格式名称; external format name
     * @param resourceName 配置资源名; configuration resource name
     * @return 对应策略; corresponding strategy
     * @throws IllegalArgumentException 格式未登记或资源名与格式不匹配时抛出; thrown when the format is unregistered or the resource name does not match
     */
    public DdcConfigFormatStrategy get(String format, String resourceName) {
        DdcConfigFormatStrategy strategy = get(format);
        if (!strategy.supports(resourceName)) {
            throw new IllegalArgumentException(
                    "DDC resource name does not match format "
                            + strategy.format() + ": " + resourceName
            );
        }
        return strategy;
    }

    /**
     * 按资源名后缀取得匹配策略。
     * Obtains the matching strategy by resource-name suffix.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @return 匹配策略; matching strategy
     * @throws IllegalArgumentException 没有策略支持该资源名时抛出; thrown when no strategy supports the resource name
     */
    public DdcConfigFormatStrategy getByResourceName(String resourceName) {
        for (DdcConfigFormatStrategy strategy : strategies.values()) {
            if (strategy.supports(resourceName)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported DDC config resource: " + resourceName
        );
    }
}
