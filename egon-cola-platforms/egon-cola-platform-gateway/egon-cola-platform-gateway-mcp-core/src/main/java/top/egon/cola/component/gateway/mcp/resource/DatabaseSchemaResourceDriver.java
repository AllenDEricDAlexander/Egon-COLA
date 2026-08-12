package top.egon.cola.component.gateway.mcp.resource;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.rejected;

/**
 * Reads an allowlisted database schema object through a supplied metadata port.
 * 补充说明 / Supplementary summary: {@code DatabaseSchemaResourceDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责数据库模式资源驱动器相关的职责与边界。
 * English supplement: {@code DatabaseSchemaResourceDriver} is a database schema resource driver driver in the current Gateway module; it owns the database schema resource driver-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class DatabaseSchemaResourceDriver
        implements McpResourceDriver {

    /**
     * 中文说明：表示 驱动器TYPE 这一固定值；它属于 {@code DatabaseSchemaResourceDriver} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value driver type; it is a state, type, or protocol value of {@code DatabaseSchemaResourceDriver} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code DatabaseSchemaResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DatabaseSchemaResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String DRIVER_TYPE = "DATABASE_SCHEMA";

    /**
     * 中文说明：保存 reader 对应的状态、依赖或配置值；字段类型为 {@code SchemaReader}，由 {@code DatabaseSchemaResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by reader; its type is {@code SchemaReader}, and {@code DatabaseSchemaResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DatabaseSchemaResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DatabaseSchemaResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final SchemaReader reader;

    /**
     * 中文说明：保存 校验器 对应的状态、依赖或配置值；字段类型为 {@code McpResourceUriValidator}，由 {@code DatabaseSchemaResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by validator; its type is {@code McpResourceUriValidator}, and {@code DatabaseSchemaResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DatabaseSchemaResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DatabaseSchemaResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpResourceUriValidator validator;

    /**
     * 中文说明：创建 {@code DatabaseSchemaResourceDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DatabaseSchemaResourceDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param reader 参数 reader；parameter reader。
     * @param validator 参数 校验器；parameter validator。
     */
    public DatabaseSchemaResourceDriver(
            SchemaReader reader,
            McpResourceUriValidator validator) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    /**
     * 中文说明：执行 驱动器Type 操作；该方法是 {@code DatabaseSchemaResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the driver type operation; this method is the invocation entry point on {@code DatabaseSchemaResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DatabaseSchemaResourceDriver.driverType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 驱动器Type 的处理结果；returns the result of the operation.
     */
    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code DatabaseSchemaResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code DatabaseSchemaResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DatabaseSchemaResourceDriver.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Content> read(ReadRequest request) {
        return Mono.fromCallable(() -> readBlocking(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 中文说明：执行 readBlocking 操作；该方法是 {@code DatabaseSchemaResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read blocking operation; this method is the invocation entry point on {@code DatabaseSchemaResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DatabaseSchemaResourceDriver.readBlocking(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 readBlocking 的处理结果；returns the result of the operation.
     */
    private Content readBlocking(ReadRequest request) throws Exception {
        String[] segments = validator.validate(request.uri())
                .getPath()
                .split("/", -1);
        if (segments.length != 4
                || !"schema".equals(segments[1])
                || segments[2].isBlank()
                || segments[3].isBlank()) {
            throw rejected("MCP database schema URI is invalid");
        }
        String schema = segments[2];
        String objectName = segments[3];
        if (!identifier(schema) || !identifier(objectName)
                || !allowed(request).contains(schema)) {
            throw rejected("MCP database schema is not allowlisted");
        }
        String content = reader.read(schema, objectName);
        if (content == null) {
            throw rejected("MCP database schema object was not found");
        }
        return bounded(
                request,
                content.getBytes(StandardCharsets.UTF_8),
                true
        );
    }

    /**
     * 中文说明：执行 allowed 操作；该方法是 {@code DatabaseSchemaResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the allowed operation; this method is the invocation entry point on {@code DatabaseSchemaResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DatabaseSchemaResourceDriver.allowed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 allowed 的处理结果；returns the result of the operation.
     */
    private Set<String> allowed(ReadRequest request) {
        String value = request.configuration().get("allowedSchemas");
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        TreeSet<String> schemas = new TreeSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(this::identifier)
                .forEach(schemas::add);
        return Set.copyOf(schemas);
    }

    /**
     * 中文说明：执行 identifier 操作；该方法是 {@code DatabaseSchemaResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identifier operation; this method is the invocation entry point on {@code DatabaseSchemaResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DatabaseSchemaResourceDriver.identifier(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 identifier 的处理结果；returns the result of the operation.
     */
    private boolean identifier(String value) {
        return value != null
                && value.matches("[A-Za-z_][A-Za-z0-9_]{0,62}");
    }

    /**
     * 中文说明：{@code SchemaReader} 是接口契约，位于当前 Gateway 模块的相关包中，负责模式Reader相关的职责与边界。
     * English summary: {@code SchemaReader} is an interface contract in the current Gateway module; it owns the schema reader-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface SchemaReader {

        /**
         * 中文说明：执行 read 操作；该方法是 {@code DatabaseSchemaResourceDriver.SchemaReader} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the read operation; this method is the invocation entry point on {@code DatabaseSchemaResourceDriver.SchemaReader} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DatabaseSchemaResourceDriver.SchemaReader.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param schema 参数 模式；parameter schema。
         * @param objectName 参数 objectName；parameter object name。
         * @return 返回 read 的处理结果；returns the result of the operation.
         */
        String read(String schema, String objectName) throws Exception;
    }
}
