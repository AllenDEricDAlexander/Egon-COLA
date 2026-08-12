package top.egon.cola.component.gateway.mcp.app;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;

import java.util.Objects;

/**
 * Resource driver for verified MCP App HTML artifacts.
 * 补充说明 / Supplementary summary: {@code AppUiResourceDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责AppUi资源驱动器相关的职责与边界。
 * English supplement: {@code AppUiResourceDriver} is a app ui resource driver driver in the current Gateway module; it owns the app ui resource driver-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class AppUiResourceDriver implements McpResourceDriver {

    /**
     * 中文说明：表示 驱动器TYPE 这一固定值；它属于 {@code AppUiResourceDriver} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value driver type; it is a state, type, or protocol value of {@code AppUiResourceDriver} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code AppUiResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AppUiResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String DRIVER_TYPE = "APP_UI";

    /**
     * 中文说明：保存 运行时 对应的状态、依赖或配置值；字段类型为 {@code McpAppRuntime}，由 {@code AppUiResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by runtime; its type is {@code McpAppRuntime}, and {@code AppUiResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AppUiResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AppUiResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpAppRuntime runtime;

    /**
     * 中文说明：创建 {@code AppUiResourceDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code AppUiResourceDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param runtime 参数 运行时；parameter runtime。
     */
    public AppUiResourceDriver(McpAppRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    /**
     * 中文说明：执行 驱动器Type 操作；该方法是 {@code AppUiResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the driver type operation; this method is the invocation entry point on {@code AppUiResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AppUiResourceDriver.driverType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 驱动器Type 的处理结果；returns the result of the operation.
     */
    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code AppUiResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code AppUiResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AppUiResourceDriver.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<Content> read(ReadRequest request) {
        return Mono.fromSupplier(() -> {
            McpAppRuntime.AppContent app = runtime.read(
                    request.serverCode(),
                    request.uri()
            );
            Content bounded = McpResourceDriver.bounded(
                    request,
                    app.content(),
                    true
            );
            return new Content(
                    bounded.uri(),
                    bounded.mimeType(),
                    bounded.data(),
                    true,
                    app.responseMetadata()
            );
        });
    }
}
