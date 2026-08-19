package top.egon.cola.component.gateway.mcp.resource.adapter;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.resource.domain.McpResourceUriValidator;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver.Content;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver.ReadRequest;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

import static top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver.rejected;

/**
 * Reads a file below a configured real root and rejects symlink escape.
 * 补充说明 / Supplementary summary: {@code ObjectStorageResourceDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责ObjectStorage资源驱动器相关的职责与边界。
 * English supplement: {@code ObjectStorageResourceDriver} is a object storage resource driver driver in the current Gateway module; it owns the object storage resource driver-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ObjectStorageResourceDriver
        implements McpResourceDriver {

    /**
     * 中文说明：表示 驱动器TYPE 这一固定值；它属于 {@code ObjectStorageResourceDriver} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value driver type; it is a state, type, or protocol value of {@code ObjectStorageResourceDriver} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ObjectStorageResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ObjectStorageResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String DRIVER_TYPE = "OBJECT_STORAGE";

    /**
     * 中文说明：保存 校验器 对应的状态、依赖或配置值；字段类型为 {@code McpResourceUriValidator}，由 {@code ObjectStorageResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by validator; its type is {@code McpResourceUriValidator}, and {@code ObjectStorageResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ObjectStorageResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ObjectStorageResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpResourceUriValidator validator;

    /**
     * 中文说明：创建 {@code ObjectStorageResourceDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ObjectStorageResourceDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param validator 参数 校验器；parameter validator。
     */
    public ObjectStorageResourceDriver(McpResourceUriValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    /**
     * 中文说明：执行 驱动器Type 操作；该方法是 {@code ObjectStorageResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the driver type operation; this method is the invocation entry point on {@code ObjectStorageResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ObjectStorageResourceDriver.driverType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 驱动器Type 的处理结果；returns the result of the operation.
     */
    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code ObjectStorageResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code ObjectStorageResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ObjectStorageResourceDriver.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Content> read(ReadRequest request) {
        return Mono.fromCallable(() -> readBlocking(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 中文说明：执行 readBlocking 操作；该方法是 {@code ObjectStorageResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read blocking operation; this method is the invocation entry point on {@code ObjectStorageResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ObjectStorageResourceDriver.readBlocking(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 readBlocking 的处理结果；returns the result of the operation.
     */
    private Content readBlocking(ReadRequest request) {
        URI uri = validator.validate(request.uri());
        String rootValue = request.configuration().get("root");
        if (rootValue == null || rootValue.isBlank()) {
            throw rejected("MCP object storage root is not configured");
        }
        try {
            Path root = Path.of(rootValue).toRealPath();
            String relativeValue = uri.getPath().substring(1);
            Path lexical = root.resolve(relativeValue).normalize();
            if (!lexical.startsWith(root)) {
                throw rejected("MCP object storage traversal is forbidden");
            }
            Path target = lexical.toRealPath();
            if (!target.startsWith(root)
                    || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw rejected("MCP object storage target is rejected");
            }
            long size = Files.size(target);
            if (size > request.maximumBytes()) {
                throw rejected("MCP resource exceeds its maximum size");
            }
            return bounded(
                    request,
                    Files.readAllBytes(target),
                    textual(request.mimeType())
            );
        } catch (McpProtocolException failure) {
            throw failure;
        } catch (Exception failure) {
            throw rejected("MCP object storage read was rejected");
        }
    }

    /**
     * 中文说明：执行 textual 操作；该方法是 {@code ObjectStorageResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the textual operation; this method is the invocation entry point on {@code ObjectStorageResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ObjectStorageResourceDriver.textual(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mimeType 参数 mimeType；parameter mime type。
     * @return 返回 textual 的处理结果；returns the result of the operation.
     */
    private boolean textual(String mimeType) {
        return mimeType.startsWith("text/")
                || "application/json".equals(mimeType)
                || mimeType.endsWith("+json")
                || "application/xml".equals(mimeType)
                || mimeType.endsWith("+xml");
    }
}
