package top.egon.cola.component.gateway.engine.rule.repository;

import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayRuleLkgRepository} 是仓储，位于当前 Gateway 模块的相关包中，负责网关规则LkgRepository相关的职责与边界。
 * English summary: {@code GatewayRuleLkgRepository} is a gateway rule lkg repository repository in the current Gateway module; it owns the gateway rule lkg repository-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRuleLkgRepository {

    /**
     * 中文说明：保存 groupDirectory 对应的状态、依赖或配置值；字段类型为 {@code Path}，由 {@code GatewayRuleLkgRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by group directory; its type is {@code Path}, and {@code GatewayRuleLkgRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleLkgRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleLkgRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Path groupDirectory;

    /**
     * 中文说明：创建 {@code GatewayRuleLkgRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRuleLkgRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param engineDataDirectory 参数 引擎DataDirectory；parameter engine data directory。
     * @param gatewayGroupCode 参数 网关GroupCode；parameter gateway group code。
     */
    public GatewayRuleLkgRepository(
            Path engineDataDirectory,
            String gatewayGroupCode) {
        if (gatewayGroupCode == null
                || !gatewayGroupCode.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("invalid gatewayGroupCode");
        }
        groupDirectory = engineDataDirectory
                .resolve("rules")
                .resolve(gatewayGroupCode);
    }

    /**
     * 中文说明：执行 persistAndActivate 操作；该方法是 {@code GatewayRuleLkgRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the persist and activate operation; this method is the invocation entry point on {@code GatewayRuleLkgRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleLkgRepository.persistAndActivate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param snapshot 参数 snapshot；parameter snapshot。
     * @param snapshotJson 参数 snapshotJson；parameter snapshot json。
     */
    public void persistAndActivate(
            GatewayRuleSnapshot snapshot,
            byte[] snapshotJson) {
        try {
            Path releases = groupDirectory.resolve("releases");
            Files.createDirectories(releases);
            Path release = releases.resolve(snapshot.releaseId() + ".json");
            Path checksum = releases.resolve(
                    snapshot.releaseId() + ".sha256"
            );
            atomicWrite(release, snapshotJson);
            atomicWrite(
                    checksum,
                    snapshot.artifactSha256().getBytes(StandardCharsets.UTF_8)
            );
            atomicWrite(
                    groupDirectory.resolve("active"),
                    snapshot.releaseId().getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "GATEWAY_RULE_LKG_WRITE_FAILED",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 loadActive 操作；该方法是 {@code GatewayRuleLkgRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load active operation; this method is the invocation entry point on {@code GatewayRuleLkgRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleLkgRepository.loadActive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 loadActive 的处理结果；returns the result of the operation.
     */
    public Optional<StoredGatewayRule> loadActive() {
        try {
            Path active = groupDirectory.resolve("active");
            if (!Files.exists(active)) {
                return Optional.empty();
            }
            String releaseId = Files.readString(active).trim();
            Path releases = groupDirectory.resolve("releases");
            byte[] json = Files.readAllBytes(
                    releases.resolve(releaseId + ".json")
            );
            String sha = Files.readString(
                    releases.resolve(releaseId + ".sha256")
            ).trim();
            return Optional.of(new StoredGatewayRule(releaseId, sha, json));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "GATEWAY_RULE_LKG_READ_FAILED",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 atomicWrite 操作；该方法是 {@code GatewayRuleLkgRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the atomic write operation; this method is the invocation entry point on {@code GatewayRuleLkgRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleLkgRepository.atomicWrite(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     * @param value 参数 值；parameter value。
     */
    private void atomicWrite(Path target, byte[] value) throws IOException {
        Path temporary = target.resolveSibling(
                target.getFileName() + ".tmp"
        );
        try (FileChannel channel = FileChannel.open(
                temporary,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(value));
            channel.force(true);
        }
        try {
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        try (FileChannel directory = FileChannel.open(
                target.getParent(),
                StandardOpenOption.READ)) {
            directory.force(true);
        }
    }

    /**
     * 中文说明：{@code StoredGatewayRule} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Stored网关规则相关的职责与边界。
     * English summary: {@code StoredGatewayRule} is an immutable data carrier in the current Gateway module; it owns the stored gateway rule-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param artifactSha256 参数 制品Sha256；parameter artifact sha256。
     * @param snapshotJson 参数 snapshotJson；parameter snapshot json。
     */
    public record StoredGatewayRule(
            /**
             * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRuleLkgRepository.StoredGatewayRule} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code GatewayRuleLkgRepository.StoredGatewayRule} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayRuleLkgRepository.StoredGatewayRule} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleLkgRepository.StoredGatewayRule}; do not couple callers to its representation when the owning type exposes an API.
             */
            String releaseId,
            /**
             * 中文说明：保存 制品Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRuleLkgRepository.StoredGatewayRule} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by artifact sha256; its type is {@code String}, and {@code GatewayRuleLkgRepository.StoredGatewayRule} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayRuleLkgRepository.StoredGatewayRule} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleLkgRepository.StoredGatewayRule}; do not couple callers to its representation when the owning type exposes an API.
             */
            String artifactSha256,
            /**
             * 中文说明：保存 snapshotJson 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code GatewayRuleLkgRepository.StoredGatewayRule} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by snapshot json; its type is {@code byte[]}, and {@code GatewayRuleLkgRepository.StoredGatewayRule} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayRuleLkgRepository.StoredGatewayRule} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleLkgRepository.StoredGatewayRule}; do not couple callers to its representation when the owning type exposes an API.
             */
            byte[] snapshotJson
    ) {

        /**
         * 中文说明：创建 {@code GatewayRuleLkgRepository.StoredGatewayRule} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayRuleLkgRepository.StoredGatewayRule} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param releaseId 参数 发布Id；parameter release id。
         * @param artifactSha256 参数 制品Sha256；parameter artifact sha256。
         * @param snapshotJson 参数 snapshotJson；parameter snapshot json。
         */
        public StoredGatewayRule {
            snapshotJson = snapshotJson.clone();
        }

        /**
         * 中文说明：执行 snapshotJson 操作；该方法是 {@code GatewayRuleLkgRepository.StoredGatewayRule} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the snapshot json operation; this method is the invocation entry point on {@code GatewayRuleLkgRepository.StoredGatewayRule} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleLkgRepository.StoredGatewayRule.snapshotJson(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 snapshotJson 的处理结果；returns the result of the operation.
         */
        @Override
        public byte[] snapshotJson() {
            return snapshotJson.clone();
        }
    }
}
