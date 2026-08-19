package top.egon.cola.component.gateway.mcp.resource.service;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy for reading one reviewed MCP resource descriptor.
 * 补充说明 / Supplementary summary: {@code McpResourceDriver} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP资源驱动器相关的职责与边界。
 * English supplement: {@code McpResourceDriver} is an interface contract in the current Gateway module; it owns the mcp resource driver-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface McpResourceDriver {

    /**
     * 中文说明：执行 驱动器Type 操作；该方法是 {@code McpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the driver type operation; this method is the invocation entry point on {@code McpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.driverType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 驱动器Type 的处理结果；returns the result of the operation.
     */
    String driverType();

    /**
     * 中文说明：执行 read 操作；该方法是 {@code McpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code McpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    Publisher<Content> read(ReadRequest request);

    /**
     * 中文说明：{@code ReadRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Read请求相关的职责与边界。
     * English summary: {@code ReadRequest} is an immutable data carrier in the current Gateway module; it owns the read request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @param uri 参数 uri；parameter uri。
     * @param mimeType 参数 mimeType；parameter mime type。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param configuration 参数 配置；parameter configuration。
     * @param uriVariables 参数 uriVariables；parameter uri variables。
     * @param maximumBytes 参数 maximumBytes；parameter maximum bytes。
     * @param attributes 参数 attributes；parameter attributes。
     */
    record ReadRequest(
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String name,
            /**
             * 中文说明：保存 uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by uri; its type is {@code String}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String uri,
            /**
             * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String mimeType,
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationId,
            /**
             * 中文说明：保存 配置 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by configuration; its type is {@code Map<String, String>}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> configuration,
            /**
             * 中文说明：保存 uriVariables 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by uri variables; its type is {@code Map<String, String>}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> uriVariables,
            /**
             * 中文说明：保存 maximumBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by maximum bytes; its type is {@code long}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            long maximumBytes,
            /**
             * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpResourceDriver.ReadRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code McpResourceDriver.ReadRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceDriver.ReadRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.ReadRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> attributes
    ) {

        /**
         * 中文说明：创建 {@code McpResourceDriver.ReadRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpResourceDriver.ReadRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param serverCode 参数 服务器Code；parameter server code。
         * @param name 参数 name；parameter name。
         * @param uri 参数 uri；parameter uri。
         * @param mimeType 参数 mimeType；parameter mime type。
         * @param operationId 参数 操作Id；parameter operation id。
         * @param configuration 参数 配置；parameter configuration。
         * @param uriVariables 参数 uriVariables；parameter uri variables。
         * @param maximumBytes 参数 maximumBytes；parameter maximum bytes。
         * @param attributes 参数 attributes；parameter attributes。
         */
        public ReadRequest {
            serverCode = required(serverCode, "serverCode");
            name = required(name, "name");
            uri = required(uri, "uri");
            mimeType = mime(mimeType);
            operationId = optional(operationId);
            configuration = configuration == null
                    ? Map.of()
                    : Map.copyOf(configuration);
            uriVariables = uriVariables == null
                    ? Map.of()
                    : Map.copyOf(uriVariables);
            if (maximumBytes < 1L || maximumBytes > 64L * 1024 * 1024) {
                throw rejected("MCP resource maximumBytes is invalid");
            }
            attributes = attributes == null ? Map.of() : Map.copyOf(
                    attributes
            );
        }
    }

    /**
     * 中文说明：{@code Content} 是类型，位于当前 Gateway 模块的相关包中，负责Content相关的职责与边界。
     * English summary: {@code Content} is a type in the current Gateway module; it owns the content-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    final class Content {

        /**
         * 中文说明：保存 uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceDriver.Content} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by uri; its type is {@code String}, and {@code McpResourceDriver.Content} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpResourceDriver.Content} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.Content}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String uri;

        /**
         * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceDriver.Content} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code McpResourceDriver.Content} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpResourceDriver.Content} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.Content}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String mimeType;

        /**
         * 中文说明：保存 data 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code McpResourceDriver.Content} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by data; its type is {@code byte[]}, and {@code McpResourceDriver.Content} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpResourceDriver.Content} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.Content}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final byte[] data;

        /**
         * 中文说明：保存 textual 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpResourceDriver.Content} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by textual; its type is {@code boolean}, and {@code McpResourceDriver.Content} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpResourceDriver.Content} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.Content}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final boolean textual;

        /**
         * 中文说明：保存 元数据 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpResourceDriver.Content} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by metadata; its type is {@code Map<String, Object>}, and {@code McpResourceDriver.Content} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpResourceDriver.Content} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceDriver.Content}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Map<String, Object> metadata;

        /**
         * 中文说明：创建 {@code McpResourceDriver.Content} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpResourceDriver.Content} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param uri 参数 uri；parameter uri。
         * @param mimeType 参数 mimeType；parameter mime type。
         * @param data 参数 data；parameter data。
         * @param textual 参数 textual；parameter textual。
         */
        public Content(
                String uri,
                String mimeType,
                byte[] data,
                boolean textual) {
            this(uri, mimeType, data, textual, Map.of());
        }

        /**
         * 中文说明：创建 {@code McpResourceDriver.Content} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpResourceDriver.Content} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param uri 参数 uri；parameter uri。
         * @param mimeType 参数 mimeType；parameter mime type。
         * @param data 参数 data；parameter data。
         * @param textual 参数 textual；parameter textual。
         * @param metadata 参数 元数据；parameter metadata。
         */
        public Content(
                String uri,
                String mimeType,
                byte[] data,
                boolean textual,
                Map<String, Object> metadata) {
            this.uri = required(uri, "uri");
            this.mimeType = mime(mimeType);
            this.data = Objects.requireNonNull(data, "data").clone();
            this.textual = textual;
            this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        /**
         * 中文说明：执行 uri 操作；该方法是 {@code McpResourceDriver.Content} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the uri operation; this method is the invocation entry point on {@code McpResourceDriver.Content} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.Content.uri(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 uri 的处理结果；returns the result of the operation.
         */
        public String uri() {
            return uri;
        }

        /**
         * 中文说明：执行 mimeType 操作；该方法是 {@code McpResourceDriver.Content} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mime type operation; this method is the invocation entry point on {@code McpResourceDriver.Content} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.Content.mimeType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 mimeType 的处理结果；returns the result of the operation.
         */
        public String mimeType() {
            return mimeType;
        }

        /**
         * 中文说明：执行 data 操作；该方法是 {@code McpResourceDriver.Content} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the data operation; this method is the invocation entry point on {@code McpResourceDriver.Content} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.Content.data(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 data 的处理结果；returns the result of the operation.
         */
        public byte[] data() {
            return data.clone();
        }

        /**
         * 中文说明：执行 textual 操作；该方法是 {@code McpResourceDriver.Content} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the textual operation; this method is the invocation entry point on {@code McpResourceDriver.Content} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.Content.textual(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 textual 的处理结果；returns the result of the operation.
         */
        public boolean textual() {
            return textual;
        }

        /**
         * 中文说明：执行 元数据 操作；该方法是 {@code McpResourceDriver.Content} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the metadata operation; this method is the invocation entry point on {@code McpResourceDriver.Content} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.Content.metadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 元数据 的处理结果；returns the result of the operation.
         */
        public Map<String, Object> metadata() {
            return metadata;
        }

        /**
         * 中文说明：执行 text 操作；该方法是 {@code McpResourceDriver.Content} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the text operation; this method is the invocation entry point on {@code McpResourceDriver.Content} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.Content.text(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 text 的处理结果；returns the result of the operation.
         */
        public String text() {
            if (!textual) {
                throw new IllegalStateException("MCP resource is binary");
            }
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /**
     * 中文说明：执行 bounded 操作；该方法是 {@code McpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bounded operation; this method is the invocation entry point on {@code McpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.bounded(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param data 参数 data；parameter data。
     * @param textual 参数 textual；parameter textual。
     * @return 返回 bounded 的处理结果；returns the result of the operation.
     */
    static Content bounded(
            ReadRequest request,
            byte[] data,
            boolean textual) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(data, "data");
        if (data.length > request.maximumBytes()) {
            throw rejected("MCP resource exceeds its maximum size");
        }
        return new Content(
                request.uri(),
                request.mimeType(),
                data,
                textual
        );
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code McpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code McpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    static McpProtocolException rejected(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_RESOURCE_REJECTED,
                message
        );
    }

    /**
     * 中文说明：执行 mime 操作；该方法是 {@code McpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mime operation; this method is the invocation entry point on {@code McpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.mime(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 mime 的处理结果；returns the result of the operation.
     */
    private static String mime(String value) {
        String result = required(value, "mimeType");
        if (result.length() > 255
                || result.indexOf('/') < 1
                || result.contains("\r")
                || result.contains("\n")) {
            throw rejected("MCP resource MIME type is invalid");
        }
        return result;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw rejected("MCP resource " + field + " is required");
        }
        return value.trim();
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code McpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code McpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceDriver.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
