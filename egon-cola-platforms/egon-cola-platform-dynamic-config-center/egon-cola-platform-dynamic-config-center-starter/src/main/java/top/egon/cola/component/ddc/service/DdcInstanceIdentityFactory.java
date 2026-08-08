package top.egon.cola.component.ddc.service;

import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Properties;

/**
 * 根据配置、运行环境和可选扩展创建稳定的 DDC 实例身份。
 * Creates a stable DDC instance identity from configuration, runtime facts, and an optional extension.
 */
public class DdcInstanceIdentityFactory {

    /** 包含构建期 SDK 版本的类路径资源。 Classpath resource containing the build-time SDK version. */
    private static final String VERSION_RESOURCE = "META-INF/egon-cola-ddc.properties";

    /** DDC 客户端配置。 DDC client configuration. */
    private final DdcProperties properties;
    /** 可覆盖实例标识生成方式的提供器。 Optional provider overriding instance identifier generation. */
    private final DdcInstanceIdProvider instanceIdProvider;

    /**
     * 创建使用配置或 UUIDv7 生成实例标识的工厂。
     * Creates a factory that obtains instance identifiers from configuration or UUIDv7 generation.
     *
     * @param properties DDC 客户端配置; DDC client configuration
     */
    public DdcInstanceIdentityFactory(DdcProperties properties) {
        this(properties, null);
    }

    /**
     * 创建支持自定义实例标识提供器的工厂。
     * Creates a factory supporting a custom instance identifier provider.
     *
     * @param properties DDC 客户端配置; DDC client configuration
     * @param instanceIdProvider 可选实例标识提供器; optional instance identifier provider
     */
    public DdcInstanceIdentityFactory(
            DdcProperties properties,
            DdcInstanceIdProvider instanceIdProvider) {
        this.properties = properties;
        this.instanceIdProvider = instanceIdProvider;
    }

    /**
     * 构建当前进程的 DDC 实例身份。
     * Builds the DDC instance identity for the current process.
     *
     * @return 当前实例身份; current instance identity
     * @throws DdcException 自定义标识为空或 SDK 版本资源无效时抛出; thrown for a blank custom identifier or invalid SDK version resource
     */
    public DdcInstanceIdentity create() {
        String host = host();
        String pid = pid();
        String instanceId = instanceId();
        return new DdcInstanceIdentity(
                instanceId,
                properties.getBizCode(),
                properties.getAppCode(),
                properties.getEnv(),
                host,
                null,
                pid,
                sdkVersion()
        );
    }

    /**
     * 按“显式配置、自定义提供器、UUIDv7”的顺序解析实例标识。
     * Resolves the instance identifier in configured, custom-provider, then UUIDv7 order.
     *
     * @return 非空实例标识; nonblank instance identifier
     */
    private String instanceId() {
        String configured = normalized(properties.getInstance().getId());
        if (configured != null) {
            return configured;
        }
        if (instanceIdProvider != null) {
            String provided = normalized(instanceIdProvider.getInstanceId());
            if (provided == null) {
                throw new DdcException("custom DDC instance id must not be blank");
            }
            return provided;
        }
        return UuidV7.string();
    }

    /**
     * 将文本去除首尾空白，并把空白值归一化为 {@code null}。
     * Trims text and normalizes blank values to {@code null}.
     *
     * @param value 待归一化文本; text to normalize
     * @return 归一化文本或 {@code null}; normalized text or {@code null}
     */
    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 从构建期资源读取有效的 DDC SDK 版本。
     * Reads a valid DDC SDK version from the build-time resource.
     *
     * @return SDK 版本; SDK version
     * @throws DdcException 资源缺失、无法读取或仍包含占位符时抛出; thrown when the resource is missing, unreadable, or unresolved
     */
    private String sdkVersion() {
        Properties version = new Properties();
        try (InputStream input = new ClassPathResource(VERSION_RESOURCE).getInputStream()) {
            version.load(input);
            String value = version.getProperty("sdk.version");
            if (value == null || value.isBlank() || value.contains("${")) {
                throw new DdcException("DDC SDK version resource is invalid");
            }
            return value;
        } catch (IOException exception) {
            throw new DdcException("load DDC SDK version failed", exception);
        }
    }

    /**
     * 获取本机地址，解析失败时回退到回环地址。
     * Returns the local host address, falling back to the loopback address on resolution failure.
     *
     * @return 主机地址; host address
     */
    private String host() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception exception) {
            return "127.0.0.1";
        }
    }

    /**
     * 获取 JVM 运行时名称作为进程标识。
     * Returns the JVM runtime name as the process identifier.
     *
     * @return JVM 运行时名称; JVM runtime name
     */
    private String pid() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }
}
