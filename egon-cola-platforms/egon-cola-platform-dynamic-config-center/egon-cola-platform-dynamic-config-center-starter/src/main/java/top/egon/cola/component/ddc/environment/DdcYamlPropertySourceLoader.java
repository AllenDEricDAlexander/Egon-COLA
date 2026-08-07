package top.egon.cola.component.ddc.environment;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import top.egon.cola.component.ddc.common.DdcChecksum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DdcYamlPropertySourceLoader {

    private static final String PROPERTY_SOURCE_PREFIX = "ddc:";

    private final YamlPropertySourceLoader delegate =
            new YamlPropertySourceLoader();

    public DdcDynamicPropertySource empty(String resourceName) {
        DdcDynamicPropertySource.Snapshot snapshot =
                new DdcDynamicPropertySource.Snapshot(
                        resourceName,
                        0L,
                        DdcChecksum.content(""),
                        Map.of()
                );
        return new DdcDynamicPropertySource(
                PROPERTY_SOURCE_PREFIX + resourceName,
                snapshot
        );
    }

    public DdcDynamicPropertySource load(String resourceName,
                                         String content,
                                         long version) throws IOException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "DDC remote YAML must not be empty"
            );
        }
        String propertySourceName = PROPERTY_SOURCE_PREFIX + resourceName;
        List<PropertySource<?>> sources = delegate.load(
                propertySourceName,
                new DdcYamlResource(resourceName, content)
        );
        if (sources.size() != 1) {
            throw new IllegalArgumentException(
                    "DDC remote YAML must contain exactly one document"
            );
        }
        if (!(sources.getFirst() instanceof EnumerablePropertySource<?> source)) {
            throw new IllegalStateException(
                    "DDC YAML loader returned a non-enumerable PropertySource"
            );
        }
        String[] propertyNames = source.getPropertyNames();
        if (propertyNames.length == 0) {
            throw new IllegalArgumentException(
                    "DDC remote YAML must contain a mapping"
            );
        }
        if (propertyNames[0].equals("document")
                || propertyNames[0].startsWith("document[")) {
            throw new IllegalArgumentException(
                    "DDC remote YAML root must be a mapping"
            );
        }
        DdcReservedConfigurationKeys.validate(source);
        Map<String, Object> rawValues = rawValues(source, propertyNames);
        DdcDynamicPropertySource.Snapshot snapshot =
                new DdcDynamicPropertySource.Snapshot(
                        resourceName,
                        version,
                        DdcChecksum.content(content),
                        rawValues
                );
        return new DdcDynamicPropertySource(propertySourceName, snapshot);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rawValues(
            EnumerablePropertySource<?> source,
            String[] propertyNames) {
        Object sourceValues = source.getSource();
        if (sourceValues instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String propertyName : propertyNames) {
            values.put(propertyName, source.getProperty(propertyName));
        }
        return values;
    }

    private static final class DdcYamlResource extends ByteArrayResource {

        private final String resourceName;

        private DdcYamlResource(String resourceName, String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
            this.resourceName = resourceName;
        }

        @Override
        public String getFilename() {
            return resourceName;
        }

        @Override
        public String getDescription() {
            return "DDC remote " + resourceName;
        }
    }
}
