package top.egon.cola.component.ddc.bootstrap;

import top.egon.cola.component.ddc.client.HttpDdcAdminClient;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 在 Spring Boot ConfigData 阶段拉取并校验唯一远程 YAML 配置。 Pulls and validates the single remote YAML configuration during Spring Boot ConfigData processing.
 */
public class DdcBootstrapClient {

    /**
     * 远程配置拉取函数。 Remote configuration pull function.
     */
    private final Supplier<List<DdcConfigValue>> pullSupplier;

    /**
     * 允许的 YAML UTF-8 最大字节数。 Maximum allowed YAML size in UTF-8 bytes.
     */
    private final long maxYamlBytes;

    /**
     * 首次拉取后保存的不可变配置快照。 Immutable configuration snapshot retained after the first pull.
     */
    private volatile List<DdcConfigValue> cachedValues;

    /**
     * 使用 DDC 属性创建通过管理端 HTTP 接口拉取配置的客户端。 Creates a client that pulls configuration through the DDC management HTTP API.
     *
     * @param properties DDC 客户端属性。 DDC client properties
     */
    public DdcBootstrapClient(DdcProperties properties) {
        this(
                () -> new HttpDdcAdminClient(properties).pull(),
                properties.getMaxYamlBytes()
        );
    }

    /**
     * 使用指定拉取函数和大小限制创建客户端，供包内测试或定制引导使用。 Creates a client with a supplied pull function and size limit for package-level testing or bootstrap customization.
     *
     * @param pullSupplier 远程配置拉取函数。 remote configuration pull function
     * @param maxYamlBytes 允许的 YAML UTF-8 最大字节数。 maximum allowed YAML size in UTF-8 bytes
     * @throws NullPointerException     拉取函数为 {@code null} 时抛出。 thrown when the pull function is {@code null}
     * @throws IllegalArgumentException 大小限制不为正数时抛出。 thrown when the size limit is not positive
     */
    DdcBootstrapClient(Supplier<List<DdcConfigValue>> pullSupplier,
                       long maxYamlBytes) {
        this.pullSupplier = Objects.requireNonNull(
                pullSupplier,
                "pullSupplier"
        );
        if (maxYamlBytes <= 0) {
            throw new IllegalArgumentException("maxYamlBytes must be positive");
        }
        this.maxYamlBytes = maxYamlBytes;
    }

    /**
     * 加载并校验指定名称的唯一 YAML 配置。 Loads and validates the single YAML configuration with the requested name.
     *
     * @param resourceName 期望的远程资源名。 expected remote resource name
     * @return 已校验的配置；作用域无配置时返回 {@code null}。 the validated configuration, or {@code null} when the scope is empty
     * @throws IllegalStateException 配置数量、名称、类型、版本或大小不符合引导约束时抛出。 thrown when count, name, type, version, or size violates bootstrap constraints
     */
    public DdcConfigValue load(String resourceName) {
        List<DdcConfigValue> values = values();
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() != 1) {
            throw new IllegalStateException(
                    "DDC scope must contain exactly one application.yml"
            );
        }
        DdcConfigValue value = values.getFirst();
        if (value == null
                || !resourceName.equals(value.getConfigKey())
                || !"YAML".equals(value.getValueType())) {
            throw new IllegalStateException(
                    "DDC scope must contain only application.yml with YAML type"
            );
        }
        if (value.getVersion() == null || value.getVersion() <= 0) {
            throw new IllegalStateException(
                    "DDC application.yml must have a positive version"
            );
        }
        String content = value.getConfigValue();
        long contentBytes = content == null
                ? 0
                : content.getBytes(StandardCharsets.UTF_8).length;
        if (contentBytes > maxYamlBytes) {
            throw new IllegalStateException(
                    "DDC application.yml exceeds the UTF-8 limit of "
                            + maxYamlBytes + " bytes"
            );
        }
        return value;
    }

    /**
     * 返回延迟加载且仅拉取一次的配置快照。 Returns the lazily loaded configuration snapshot, pulling at most once.
     *
     * @return 不可变配置列表。 immutable configuration list
     */
    private List<DdcConfigValue> values() {
        List<DdcConfigValue> current = cachedValues;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedValues == null) {
                List<DdcConfigValue> pulled = pullSupplier.get();
                cachedValues = pulled == null ? List.of() : List.copyOf(pulled);
            }
            return cachedValues;
        }
    }
}
