package top.egon.cola.component.gateway.engine.common.security.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;

/**
 * 中文说明：{@code GatewayTransportSecurity} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关传输安全相关的职责与边界。
 * English summary: {@code GatewayTransportSecurity} is an immutable data carrier in the current Gateway module; it owns the gateway transport security-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param enabled 参数 enabled；parameter enabled。
 * @param developmentPlaintext 参数 developmentPlaintext；parameter development plaintext。
 * @param certificateChainPath 参数 certificateChainPath；parameter certificate chain path。
 * @param privateKeyPath 参数 private键Path；parameter private key path。
 * @param trustCertificateCollectionPath 参数 trustCertificateCollectionPath；parameter trust certificate collection path。
 * @param clientCertificateRequired 参数 客户端CertificateRequired；parameter client certificate required。
 */
public record GatewayTransportSecurity(
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayTransportSecurity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayTransportSecurity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTransportSecurity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportSecurity}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 developmentPlaintext 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayTransportSecurity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by development plaintext; its type is {@code boolean}, and {@code GatewayTransportSecurity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTransportSecurity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportSecurity}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean developmentPlaintext,
        /**
         * 中文说明：保存 certificateChainPath 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTransportSecurity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by certificate chain path; its type is {@code String}, and {@code GatewayTransportSecurity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTransportSecurity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportSecurity}; do not couple callers to its representation when the owning type exposes an API.
         */
        String certificateChainPath,
        /**
         * 中文说明：保存 private键Path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTransportSecurity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by private key path; its type is {@code String}, and {@code GatewayTransportSecurity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTransportSecurity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportSecurity}; do not couple callers to its representation when the owning type exposes an API.
         */
        String privateKeyPath,
        /**
         * 中文说明：保存 trustCertificateCollectionPath 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTransportSecurity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trust certificate collection path; its type is {@code String}, and {@code GatewayTransportSecurity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTransportSecurity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportSecurity}; do not couple callers to its representation when the owning type exposes an API.
         */
        String trustCertificateCollectionPath,
        /**
         * 中文说明：保存 客户端CertificateRequired 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayTransportSecurity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by client certificate required; its type is {@code boolean}, and {@code GatewayTransportSecurity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTransportSecurity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTransportSecurity}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean clientCertificateRequired
) {

    /**
     * 中文说明：创建 {@code GatewayTransportSecurity} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTransportSecurity} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param enabled 参数 enabled；parameter enabled。
     * @param developmentPlaintext 参数 developmentPlaintext；parameter development plaintext。
     * @param certificateChainPath 参数 certificateChainPath；parameter certificate chain path。
     * @param privateKeyPath 参数 private键Path；parameter private key path。
     * @param trustCertificateCollectionPath 参数 trustCertificateCollectionPath；parameter trust certificate collection path。
     * @param clientCertificateRequired 参数 客户端CertificateRequired；parameter client certificate required。
     */
    public GatewayTransportSecurity {
        if (!enabled) {
            if (!developmentPlaintext) {
                throw new IllegalArgumentException(
                        "plaintext transport requires explicit development mode"
                );
            }
        } else {
            requireReadable(certificateChainPath, "certificate chain");
            requireReadable(privateKeyPath, "private key");
            if (clientCertificateRequired) {
                requireReadable(
                        trustCertificateCollectionPath,
                        "trust certificate collection"
                );
            }
        }
    }

    /**
     * 中文说明：执行 developmentPlaintextConfig 操作；该方法是 {@code GatewayTransportSecurity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the development plaintext config operation; this method is the invocation entry point on {@code GatewayTransportSecurity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurity.developmentPlaintextConfig(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 developmentPlaintextConfig 的处理结果；returns the result of the operation.
     */
    public static GatewayTransportSecurity developmentPlaintextConfig() {
        return new GatewayTransportSecurity(
                false,
                true,
                null,
                null,
                null,
                false
        );
    }

    /**
     * 中文说明：执行 certificateChainFile 操作；该方法是 {@code GatewayTransportSecurity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the certificate chain file operation; this method is the invocation entry point on {@code GatewayTransportSecurity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurity.certificateChainFile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 certificateChainFile 的处理结果；returns the result of the operation.
     */
    public Path certificateChainFile() {
        return Path.of(certificateChainPath);
    }

    /**
     * 中文说明：执行 private键File 操作；该方法是 {@code GatewayTransportSecurity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the private key file operation; this method is the invocation entry point on {@code GatewayTransportSecurity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurity.privateKeyFile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 private键File 的处理结果；returns the result of the operation.
     */
    public Path privateKeyFile() {
        return Path.of(privateKeyPath);
    }

    /**
     * 中文说明：执行 trustCertificateCollectionFile 操作；该方法是 {@code GatewayTransportSecurity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trust certificate collection file operation; this method is the invocation entry point on {@code GatewayTransportSecurity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurity.trustCertificateCollectionFile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 trustCertificateCollectionFile 的处理结果；returns the result of the operation.
     */
    public Path trustCertificateCollectionFile() {
        return Path.of(trustCertificateCollectionPath);
    }

    /**
     * 中文说明：执行 certificateExpiryEpochSeconds 操作；该方法是 {@code GatewayTransportSecurity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the certificate expiry epoch seconds operation; this method is the invocation entry point on {@code GatewayTransportSecurity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurity.certificateExpiryEpochSeconds(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 certificateExpiryEpochSeconds 的处理结果；returns the result of the operation.
     */
    public long certificateExpiryEpochSeconds() {
        if (!enabled) {
            return 0;
        }
        try (InputStream input = Files.newInputStream(
                certificateChainFile()
        )) {
            X509Certificate certificate = (X509Certificate)
                    CertificateFactory.getInstance("X.509")
                            .generateCertificate(input);
            return certificate.getNotAfter().toInstant().getEpochSecond();
        } catch (IOException | CertificateException failure) {
            throw new IllegalStateException(
                    "failed to read TLS certificate expiry",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 secondsUntilExpiry 操作；该方法是 {@code GatewayTransportSecurity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the seconds until expiry operation; this method is the invocation entry point on {@code GatewayTransportSecurity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurity.secondsUntilExpiry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 secondsUntilExpiry 的处理结果；returns the result of the operation.
     */
    public long secondsUntilExpiry(Instant now) {
        if (!enabled) {
            return 0;
        }
        return certificateExpiryEpochSeconds() - now.getEpochSecond();
    }

    /**
     * 中文说明：执行 requireReadable 操作；该方法是 {@code GatewayTransportSecurity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require readable operation; this method is the invocation entry point on {@code GatewayTransportSecurity} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTransportSecurity.requireReadable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param path 参数 path；parameter path。
     * @param description 参数 description；parameter description。
     */
    private static void requireReadable(String path, String description) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    description + " path is required"
            );
        }
        Path file = Path.of(path);
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException(
                    description + " file is not readable: " + file
            );
        }
    }
}
