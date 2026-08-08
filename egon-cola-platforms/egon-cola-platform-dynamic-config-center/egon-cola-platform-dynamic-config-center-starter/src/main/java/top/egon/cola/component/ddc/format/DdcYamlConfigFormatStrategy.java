package top.egon.cola.component.ddc.format;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.environment.DdcReservedConfigurationKeys;
import top.egon.cola.component.ddc.model.enums.DdcConfigFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 Spring Boot 原生加载器将远程 YAML 解析为保留来源信息的 DDC 动态属性源。
 * Parses remote YAML into an origin-aware DDC dynamic property source with Spring Boot's native loader.
 */
public final class DdcYamlConfigFormatStrategy
        implements DdcConfigFormatStrategy {

    /**
     * Starter 支持的远程资源名。 Remote resource name supported by the starter.
     */
    public static final String DEFAULT_RESOURCE_NAME = "application.yml";

    /**
     * 动态属性源名称前缀。 Dynamic property-source name prefix.
     */
    private static final String PROPERTY_SOURCE_PREFIX = "ddc:";

    /**
     * Spring Boot 原生 YAML 属性源加载器。 Spring Boot native YAML property-source loader.
     */
    private final YamlPropertySourceLoader delegate =
            new YamlPropertySourceLoader();

    /**
     * 返回当前策略处理的 YAML 格式。 Returns the YAML format handled by this strategy.
     *
     * @return YAML 配置格式; YAML configuration format
     */
    @Override
    public DdcConfigFormat format() {
        return DdcConfigFormat.YAML;
    }

    /**
     * 创建版本为零且无属性的动态属性源。 Creates a version-zero dynamic property source with no properties.
     *
     * @param resourceName 远程资源名。 remote resource name
     * @return 空动态属性源。 empty dynamic property source
     */
    @Override
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

    /**
     * 解析并校验非空、单文档、根映射且不含保留键的 YAML。 Parses and validates non-empty, single-document, mapping-root YAML without reserved keys.
     *
     * @param resourceName 远程资源名。 remote resource name
     * @param content      YAML 文本。 YAML content
     * @param version      远程配置版本。 remote configuration version
     * @return 包含内容摘要和来源信息的动态属性源。 dynamic property source containing content checksum and origin data
     * @throws IOException              Spring YAML 加载器读取资源失败时抛出。 thrown when Spring's YAML loader cannot read the resource
     * @throws IllegalArgumentException YAML 为空、多文档、非映射根或包含保留键时抛出。 thrown when YAML is empty, multi-document, non-mapping-root, or contains reserved keys
     * @throws IllegalStateException    加载器返回不可枚举属性源时抛出。 thrown when the delegate returns a non-enumerable property source
     */
    @Override
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

    /**
     * 优先复制加载器底层映射，以保留 {@code OriginTrackedValue}；否则逐项读取属性。 Prefers copying the loader's backing map to preserve {@code OriginTrackedValue}; otherwise reads properties individually.
     *
     * @param source        可枚举属性源。 enumerable property source
     * @param propertyNames 属性名数组。 property-name array
     * @return 保持遍历顺序的原始属性映射。 insertion-ordered raw property map
     */
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

    /**
     * 用 UTF-8 字节承载远程 YAML，并提供稳定文件名和诊断描述。 Carries remote YAML as UTF-8 bytes with a stable filename and diagnostic description.
     */
    private static final class DdcYamlResource extends ByteArrayResource {

        /**
         * 用于 YAML 解析和来源报告的资源名。 Resource name used for YAML parsing and origin reporting.
         */
        private final String resourceName;

        /**
         * 创建内存 YAML 资源。 Creates an in-memory YAML resource.
         *
         * @param resourceName 远程资源名。 remote resource name
         * @param content      YAML 文本。 YAML content
         */
        private DdcYamlResource(String resourceName, String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
            this.resourceName = resourceName;
        }

        /**
         * 返回远程资源名。 Returns the remote resource name.
         *
         * @return 远程资源名。 remote resource name
         */
        @Override
        public String getFilename() {
            return resourceName;
        }

        /**
         * 返回标明资源来自 DDC 远端的诊断描述。 Returns a diagnostic description identifying the resource as remote DDC content.
         *
         * @return 远程 DDC 资源描述。 remote DDC resource description
         */
        @Override
        public String getDescription() {
            return "DDC remote " + resourceName;
        }
    }
}
