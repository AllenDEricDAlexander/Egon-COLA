package top.egon.cola.component.rpc.ddc.configdata;

import org.springframework.lang.Nullable;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.format.DdcConfigFormatStrategyRegistry;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 在 Spring Boot ConfigData 阶段拉取并校验唯一远程 YAML 配置。 Pulls and validates the single remote YAML configuration during Spring Boot ConfigData processing.
 */
public class DdcConfigDataFetcher {

    /**
     * 远程配置拉取函数。 Remote configuration pull function.
     */
    private final Supplier<List<DdcConfigValue>> pullSupplier;

    /**
     * 允许的 YAML UTF-8 最大字节数。 Maximum allowed YAML size in UTF-8 bytes.
     */
    private final long maxConfigBytes;

    /**
     * 校验配置格式与资源名匹配关系的策略注册表。
     * Strategy registry validating configuration format and resource-name compatibility.
     */
    private final DdcConfigFormatStrategyRegistry formatStrategies;

    /**
     * 首次拉取后保存的不可变配置快照。 Immutable configuration snapshot retained after the first pull.
     */
    private volatile List<DdcConfigValue> cachedValues;

    /**
     * 使用本地 DDC 与 Direct RPC 属性创建一次性引导客户端。
     * Creates a one-shot bootstrap client from local DDC and Direct RPC settings.
     *
     * @param properties    DDC 作用域与内容约束 / DDC scope and content constraints
     * @param rpcProperties DDC Direct RPC 引导属性 / DDC Direct RPC bootstrap settings
     */
    public DdcConfigDataFetcher(
            DdcProperties properties,
            DdcRpcProperties rpcProperties) {
        this(
                pullSupplier(properties, rpcProperties),
                properties.getMaxConfigBytes(),
                DdcConfigFormatStrategyRegistry.defaults()
        );
    }

    /**
     * 使用指定拉取函数和大小限制创建客户端，供包内测试或定制引导使用。 Creates a client with a supplied pull function and size limit for package-level testing or bootstrap customization.
     *
     * @param pullSupplier 远程配置拉取函数。 remote configuration pull function
     * @param maxConfigBytes 允许的 YAML UTF-8 最大字节数。 maximum allowed YAML size in UTF-8 bytes
     * @throws NullPointerException     拉取函数为 {@code null} 时抛出。 thrown when the pull function is {@code null}
     * @throws IllegalArgumentException 大小限制不为正数时抛出。 thrown when the size limit is not positive
     */
    DdcConfigDataFetcher(Supplier<List<DdcConfigValue>> pullSupplier,
                       long maxConfigBytes) {
        this(
                pullSupplier,
                maxConfigBytes,
                DdcConfigFormatStrategyRegistry.defaults()
        );
    }

    /**
     * 使用指定拉取函数、大小限制和格式策略注册表创建客户端。
     * Creates a client with the supplied pull function, size limit, and format-strategy registry.
     *
     * @param pullSupplier    远程配置拉取函数; remote configuration pull function
     * @param maxConfigBytes    允许的 YAML UTF-8 最大字节数; maximum allowed YAML size in UTF-8 bytes
     * @param formatStrategies 配置格式策略注册表; configuration-format strategy registry
     */
    DdcConfigDataFetcher(
            Supplier<List<DdcConfigValue>> pullSupplier,
            long maxConfigBytes,
            DdcConfigFormatStrategyRegistry formatStrategies) {
        this.pullSupplier = Objects.requireNonNull(
                pullSupplier,
                "pullSupplier"
        );
        if (maxConfigBytes <= 0) {
            throw new IllegalArgumentException("maxConfigBytes must be positive");
        }
        this.maxConfigBytes = maxConfigBytes;
        this.formatStrategies = Objects.requireNonNull(
                formatStrategies,
                "formatStrategies"
        );
    }

    /**
     * 加载并校验指定名称的唯一 YAML 配置。 Loads and validates the single YAML configuration with the requested name.
     *
     * @param resourceName 期望的远程资源名。 expected remote resource name
     * @return 已校验的配置；作用域无配置时返回 {@code null}。 the validated configuration, or {@code null} when the scope is empty
     * @throws IllegalStateException 配置数量、名称、类型、版本或大小不符合引导约束时抛出。 thrown when count, name, type, version, or size violates bootstrap constraints
     */
    @Nullable
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
        if (value == null || !resourceName.equals(value.getResourceName())) {
            throw new IllegalStateException(
                    "DDC scope must contain only application.yml with YAML type"
            );
        }
        try {
            formatStrategies.get(value.getFormat(), resourceName);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "DDC scope must contain only application.yml with YAML type",
                    exception
            );
        }
        if (value.getVersion() == null || value.getVersion() <= 0) {
            throw new IllegalStateException(
                    "DDC application.yml must have a positive version"
            );
        }
        String content = value.getContent();
        long contentBytes = content == null
                ? 0
                : content.getBytes(StandardCharsets.UTF_8).length;
        if (contentBytes > maxConfigBytes) {
            throw new IllegalStateException(
                    "DDC application.yml exceeds the UTF-8 limit of "
                            + maxConfigBytes + " bytes"
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

    /** 创建每次拉取后可靠关闭 Channel 的引导调用。 / Creates a bootstrap pull that always closes its Channel. */
    private static Supplier<List<DdcConfigValue>> pullSupplier(
            DdcProperties properties,
            DdcRpcProperties rpcProperties) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(rpcProperties, "rpcProperties");
        DdcRpcClientFactory factory = new DdcRpcClientFactory(
                rpcProperties,
                properties,
                bootstrapIdentity(properties)
        );
        return () -> {
            try (DdcRpcClientHandle<DdcConfigClient> handle =
                         factory.configClient()) {
                return handle.client().pull();
            }
        };
    }

    /** 构造不依赖 ApplicationContext 的引导调用身份。 / Builds bootstrap identity without an ApplicationContext. */
    private static RpcProcessIdentity bootstrapIdentity(
            DdcProperties properties) {
        String host = localHost();
        long pid = ProcessHandle.current().pid();
        String instanceId = properties.getInstance().getId();
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = properties.getAppCode()
                    + "-configdata-" + host + '-' + pid;
        }
        return new RpcProcessIdentity(
                properties.getAppCode(),
                properties.getEnv(),
                host,
                pid,
                instanceId
        );
    }

    /** 解析本机地址，失败时使用回环地址。 / Resolves the local host with a loopback fallback. */
    private static String localHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "127.0.0.1";
        }
    }
}
