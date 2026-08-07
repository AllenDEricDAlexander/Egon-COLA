package top.egon.cola.component.ddc.environment;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.List;

public final class DdcReservedConfigurationKeys {

    private static final List<ConfigurationPropertyName> RESERVED = List.of(
            ConfigurationPropertyName.of("egon.cola.component.ddc"),
            ConfigurationPropertyName.of("spring.config"),
            ConfigurationPropertyName.of("spring.profiles.active"),
            ConfigurationPropertyName.of("spring.profiles.default"),
            ConfigurationPropertyName.of("spring.profiles.include"),
            ConfigurationPropertyName.of("spring.profiles.group")
    );

    private DdcReservedConfigurationKeys() {
    }

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

    public static boolean isReserved(String propertyName) {
        return isReserved(canonical(propertyName));
    }

    private static boolean isReserved(ConfigurationPropertyName candidate) {
        return RESERVED.stream().anyMatch(reserved ->
                reserved.equals(candidate) || reserved.isAncestorOf(candidate));
    }

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
