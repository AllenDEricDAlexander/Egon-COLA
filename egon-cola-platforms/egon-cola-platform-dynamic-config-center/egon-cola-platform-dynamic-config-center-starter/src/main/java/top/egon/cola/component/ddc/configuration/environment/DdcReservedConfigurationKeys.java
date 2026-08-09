package top.egon.cola.component.ddc.configuration.environment;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.List;

/**
 * 保护必须由本地引导配置控制、不能由远程 YAML 覆盖的属性前缀。 Protects property prefixes that must remain under local bootstrap control and cannot be overridden by remote YAML.
 */
public final class DdcReservedConfigurationKeys {

    /**
     * DDC 自身引导配置及 Spring ConfigData/Profile 控制键。 DDC bootstrap and Spring ConfigData/profile control keys.
     */
    private static final List<ConfigurationPropertyName> RESERVED = List.of(
            ConfigurationPropertyName.of("egon.cola.component.ddc"),
            ConfigurationPropertyName.of("spring.config"),
            ConfigurationPropertyName.of("spring.profiles.active"),
            ConfigurationPropertyName.of("spring.profiles.default"),
            ConfigurationPropertyName.of("spring.profiles.include"),
            ConfigurationPropertyName.of("spring.profiles.group")
    );

    /**
     * 禁止实例化保留键工具类。 Prevents instantiation of the reserved-key utility.
     */
    private DdcReservedConfigurationKeys() {
    }

    /**
     * 校验属性源中不包含保留键或其后代键。 Validates that a property source contains neither reserved keys nor their descendants.
     *
     * @param propertySource 待校验的可枚举属性源。 enumerable property source to validate
     * @throws IllegalArgumentException 检测到保留键时抛出，并尽量包含来源位置。 thrown when a reserved key is found, including its origin when available
     */
    public static void validate(EnumerablePropertySource<?> propertySource) {
        for (String propertyName : propertySource.getPropertyNames()) {
            ConfigurationPropertyName candidate = canonical(propertyName);
            if (isReserved(candidate)) {
                Origin origin = OriginLookup.getOrigin(
                        propertySource,
                        propertyName
                );
                String location = origin == null ? "" : " at " + origin;
                throw new IllegalArgumentException(
                        "DDC remote YAML contains reserved key '"
                                + candidate + '\'' + location
                );
            }
        }
    }

    /**
     * 判断给定宽松格式属性名是否属于保留前缀。 Determines whether a relaxed-form property name belongs to a reserved prefix.
     *
     * @param propertyName 待判断属性名。 property name to inspect
     * @return 属性受保护时为 {@code true}。 {@code true} when the property is protected
     */
    public static boolean isReserved(String propertyName) {
        return isReserved(canonical(propertyName));
    }

    /**
     * 判断规范属性名是否等于保留键或位于其下。 Determines whether a canonical name equals or descends from a reserved key.
     *
     * @param candidate 规范属性名。 canonical property name
     * @return 属于保留范围时为 {@code true}。 {@code true} when within a reserved range
     */
    private static boolean isReserved(ConfigurationPropertyName candidate) {
        return RESERVED.stream().anyMatch(reserved ->
                reserved.equals(candidate) || reserved.isAncestorOf(candidate));
    }

    /**
     * 将下划线等宽松写法转换为 Spring 规范属性名。 Converts relaxed underscore forms into a canonical Spring property name.
     *
     * @param propertyName 原始属性名。 raw property name
     * @return 规范属性名，空输入返回 {@link ConfigurationPropertyName#EMPTY}。 canonical name, or {@link ConfigurationPropertyName#EMPTY} for blank input
     */
    private static ConfigurationPropertyName canonical(String propertyName) {
        if (propertyName == null || propertyName.isBlank()) {
            return ConfigurationPropertyName.EMPTY;
        }
        return ConfigurationPropertyName.adapt(
                propertyName.replace('_', '.'),
                '.'
        );
    }
}
