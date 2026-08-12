package top.egon.cola.component.gateway.mcp.app;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resolves active MCP Apps and revalidates stored bytes on every read.
 * 补充说明 / Supplementary summary: {@code McpAppRuntime} 是运行时组件，位于当前 Gateway 模块的相关包中，负责MCPApp运行时相关的职责与边界。
 * English supplement: {@code McpAppRuntime} is a mcp app runtime runtime in the current Gateway module; it owns the mcp app runtime-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpAppRuntime {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code McpAppRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code McpAppRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpAppRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 artifacts 对应的状态、依赖或配置值；字段类型为 {@code McpAppArtifactStore.Reader}，由 {@code McpAppRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifacts; its type is {@code McpAppArtifactStore.Reader}, and {@code McpAppRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpAppRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpAppArtifactStore.Reader artifacts;

    /**
     * 中文说明：保存 校验器 对应的状态、依赖或配置值；字段类型为 {@code McpAppSecurityValidator}，由 {@code McpAppRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by validator; its type is {@code McpAppSecurityValidator}, and {@code McpAppRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpAppRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpAppSecurityValidator validator;

    /**
     * 中文说明：创建 {@code McpAppRuntime} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpAppRuntime} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param artifacts 参数 artifacts；parameter artifacts。
     * @param validator 参数 校验器；parameter validator。
     */
    public McpAppRuntime(
            Supplier<CompiledMcpRules> rules,
            McpAppArtifactStore.Reader artifacts,
            McpAppSecurityValidator validator) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code McpAppRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code McpAppRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppRuntime.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    public AppContent read(String serverCode, String resourceUri) {
        McpRuntimeApp app = active().appsByQualifiedName().values().stream()
                .filter(McpRuntimeApp::enabled)
                .filter(candidate -> candidate.serverCode().equals(serverCode))
                .filter(candidate -> candidate.resourceUri().equals(resourceUri))
                .findFirst()
                .orElseThrow(() -> McpResourceDriver.rejected(
                        "MCP App resource was not found"
                ));
        McpAppArtifactStore.ArtifactContent artifact = artifacts.read(
                new McpAppArtifactStore.ReadRequest(
                        app.artifactReference(),
                        app.artifactSha256(),
                        app.artifactSizeBytes()
                )
        );
        validator.validate(app, artifact);
        return new AppContent(app, artifact.content(), metadata(app));
    }

    /**
     * 中文说明：执行 元数据 操作；该方法是 {@code McpAppRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the metadata operation; this method is the invocation entry point on {@code McpAppRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppRuntime.metadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param app 参数 app；parameter app。
     * @return 返回 元数据 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> metadata(McpRuntimeApp app) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("sandbox", "allow-scripts");
        value.put("content-security-policy", app.contentSecurityPolicy());
        value.put("cache-control", "no-store");
        value.put("x-content-type-options", "nosniff");
        value.put("cookies", "disabled");
        value.put("permissions", app.permissions());
        value.put("allowed-tools", app.allowedTools());
        return Map.copyOf(value);
    }

    /**
     * 中文说明：执行 active 操作；该方法是 {@code McpAppRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code McpAppRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppRuntime.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }

    /**
     * 中文说明：{@code AppContent} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责AppContent相关的职责与边界。
     * English summary: {@code AppContent} is an immutable data carrier in the current Gateway module; it owns the app content-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param app 参数 app；parameter app。
     * @param content 参数 content；parameter content。
     * @param responseMetadata 参数 响应元数据；parameter response metadata。
     */
    public record AppContent(
            /**
             * 中文说明：保存 app 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeApp}，由 {@code McpAppRuntime.AppContent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by app; its type is {@code McpRuntimeApp}, and {@code McpAppRuntime.AppContent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppRuntime.AppContent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppRuntime.AppContent}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpRuntimeApp app,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code McpAppRuntime.AppContent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code byte[]}, and {@code McpAppRuntime.AppContent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppRuntime.AppContent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppRuntime.AppContent}; do not couple callers to its representation when the owning type exposes an API.
             */
            byte[] content,
            /**
             * 中文说明：保存 响应元数据 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpAppRuntime.AppContent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by response metadata; its type is {@code Map<String, Object>}, and {@code McpAppRuntime.AppContent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppRuntime.AppContent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppRuntime.AppContent}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> responseMetadata
    ) {

        /**
         * 中文说明：创建 {@code McpAppRuntime.AppContent} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpAppRuntime.AppContent} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param app 参数 app；parameter app。
         * @param content 参数 content；parameter content。
         * @param responseMetadata 参数 响应元数据；parameter response metadata。
         */
        public AppContent {
            app = Objects.requireNonNull(app, "app");
            content = Objects.requireNonNull(content, "content").clone();
            responseMetadata = Map.copyOf(responseMetadata);
        }

        /**
         * 中文说明：执行 content 操作；该方法是 {@code McpAppRuntime.AppContent} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the content operation; this method is the invocation entry point on {@code McpAppRuntime.AppContent} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpAppRuntime.AppContent.content(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 content 的处理结果；returns the result of the operation.
         */
        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
