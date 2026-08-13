package top.egon.cola.component.gateway.admin.reporting.service;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import top.egon.cola.component.gateway.admin.reporting.service.GatewaySchemaValidationState;
/**
 * Validates the v2 operation schemas received from a Gateway starter.
 *
 * <p>The report is the source of truth for both ordinary Gateway operations
 * and declarative MCP tools, so this validator deliberately rejects schema
 * shapes that cannot be interpreted by the Gateway runtime.
 * 补充说明 / Supplementary summary: {@code GatewayOperationSchemaValidator} 是校验器，位于当前 Gateway 模块的相关包中，负责网关操作模式校验器相关的职责与边界。
 * English supplement: {@code GatewayOperationSchemaValidator} is a gateway operation schema validator validator in the current Gateway module; it owns the gateway operation schema validator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayOperationSchemaValidator {

    /**
     * 中文说明：表示 请求模型 这一固定值；它属于 {@code GatewayOperationSchemaValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value request model; it is a state, type, or protocol value of {@code GatewayOperationSchemaValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOperationSchemaValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOperationSchemaValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String REQUEST_MODEL =
            "gateway-operation-request/v2";

    /**
     * 中文说明：表示 响应模型 这一固定值；它属于 {@code GatewayOperationSchemaValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value response model; it is a state, type, or protocol value of {@code GatewayOperationSchemaValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOperationSchemaValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOperationSchemaValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String RESPONSE_MODEL =
            "gateway-operation-response/v2";

    /**
     * 中文说明：表示 HTTPLOCATIONS 这一固定值；它属于 {@code GatewayOperationSchemaValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value http locations; it is a state, type, or protocol value of {@code GatewayOperationSchemaValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOperationSchemaValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOperationSchemaValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> HTTP_LOCATIONS = Set.of(
            "path", "query", "header", "cookie", "body", "part"
    );

    /**
     * 中文说明：表示 MAXDEPTH 这一固定值；它属于 {@code GatewayOperationSchemaValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max depth; it is a state, type, or protocol value of {@code GatewayOperationSchemaValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOperationSchemaValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOperationSchemaValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_DEPTH = 32;

    /**
     * 中文说明：表示 MAXNODES 这一固定值；它属于 {@code GatewayOperationSchemaValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max nodes; it is a state, type, or protocol value of {@code GatewayOperationSchemaValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOperationSchemaValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOperationSchemaValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_NODES = 10_000;

    /**
     * 中文说明：表示 MAXSERIALIZEDBYTES 这一固定值；它属于 {@code GatewayOperationSchemaValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max serialized bytes; it is a state, type, or protocol value of {@code GatewayOperationSchemaValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOperationSchemaValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOperationSchemaValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_SERIALIZED_BYTES = 2 * 1024 * 1024;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code GatewayOperationSchemaValidator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code GatewayOperationSchemaValidator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayOperationSchemaValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayOperationSchemaValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code GatewayOperationSchemaValidator} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayOperationSchemaValidator} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public GatewayOperationSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationKey 参数 操作键；parameter operation key。
     * @param protocol 参数 protocol；parameter protocol。
     * @param requestSchema 参数 请求模式；parameter request schema。
     * @param responseSchema 参数 响应模式；parameter response schema。
     * @param attributes 参数 attributes；parameter attributes。
     */
    public void validate(
            String operationKey,
            String protocol,
            Map<String, Object> requestSchema,
            Map<String, Object> responseSchema,
            Map<String, Object> attributes) {
        String identity = operationKey == null || operationKey.isBlank()
                ? "operation"
                : operationKey;
        requireSchema(identity + ".requestSchema", requestSchema, REQUEST_MODEL);
        requireSchema(identity + ".responseSchema", responseSchema, RESPONSE_MODEL);
        boolean mcp = registeredForMcp(attributes);
        validateNode(identity + ".requestSchema", requestSchema, requestSchema,
                protocol, mcp, new GatewaySchemaValidationState());
        validateNode(identity + ".responseSchema", responseSchema, responseSchema,
                protocol, false, new GatewaySchemaValidationState());
        if ("HTTP".equalsIgnoreCase(protocol)) {
            validateHttpRoot(identity, requestSchema, mcp);
        } else if ("RPC".equalsIgnoreCase(protocol)) {
            validateObjectRoot(identity + ".requestSchema", requestSchema);
            validateObjectRoot(identity + ".responseSchema", responseSchema);
        } else {
            throw invalid(identity, "unsupported operation protocol: " + protocol);
        }
        if (mcp && Boolean.TRUE.equals(attributes.get("streaming"))) {
            throw invalid(identity, "streaming operations are unsupported");
        }
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     */
    public void validate(GatewayInterfaceDefinitionReport.Operation operation) {
        validate(
                operation.operationKey(),
                operation.protocol(),
                operation.requestSchema(),
                operation.responseSchema(),
                operation.attributes()
        );
    }

    /**
     * 中文说明：执行 require模式 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require schema operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.requireSchema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param schema 参数 模式；parameter schema。
     * @param expectedModel 参数 expected模型；parameter expected model。
     */
    private void requireSchema(
            String identity,
            Map<String, Object> schema,
            String expectedModel) {
        if (schema == null || schema.isEmpty()) {
            throw invalid(identity, "schema is required");
        }
        Object model = schema.get("x-egon-schema-model");
        if (!expectedModel.equals(model)) {
            throw invalid(identity, "schema model must be " + expectedModel);
        }
        try {
            if (objectMapper.writeValueAsBytes(schema).length
                    > MAX_SERIALIZED_BYTES) {
                throw invalid(identity, "schema exceeds serialized size limit");
            }
        } catch (JsonProcessingException failure) {
            throw invalid(identity, "schema is not serializable", failure);
        }
    }

    /**
     * 中文说明：执行 validateHttpRoot 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate http root operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.validateHttpRoot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param schema 参数 模式；parameter schema。
     * @param mcp 参数 MCP；parameter mcp。
     */
    private void validateHttpRoot(
            String identity,
            Map<String, Object> schema,
            boolean mcp) {
        validateObjectRoot(identity + ".requestSchema", schema);
        Map<String, Object> properties = map(schema.get("properties"));
        if (properties == null) {
            throw invalid(identity, "HTTP request properties are required");
        }
        for (String location : properties.keySet()) {
            if (!HTTP_LOCATIONS.contains(location)) {
                throw invalid(identity, "unknown HTTP location group: " + location);
            }
            Object value = properties.get(location);
            if (!(value instanceof Map<?, ?>)) {
                throw invalid(identity, "HTTP location group must be an object: " + location);
            }
            if (!"body".equals(location)) {
                validateObjectRoot(identity + ".requestSchema." + location,
                        cast(value));
            }
            if (mcp) {
                validateMcpLocation(identity, location, cast(value));
            }
        }
        validateRequired(identity + ".requestSchema", schema, properties.keySet());
    }

    /**
     * 中文说明：执行 validateMCPLocation 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate mcp location operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.validateMcpLocation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param location 参数 location；parameter location。
     * @param schema 参数 模式；parameter schema。
     */
    private void validateMcpLocation(
            String identity,
            String location,
            Map<String, Object> schema) {
        if ("part".equals(location)) {
            throw invalid(identity, "PART parameters are unsupported");
        }
        if (!"header".equals(location) && !"cookie".equals(location)) {
            return;
        }
        Map<String, Object> properties = map(schema.get("properties"));
        if (properties == null) {
            return;
        }
        Set<String> required = requiredNames(identity + "." + location, schema,
                properties.keySet());
        for (String name : required) {
            if (!("header".equals(location)
                    && "authorization".equalsIgnoreCase(name))) {
                throw invalid(identity, "required " + location.toUpperCase()
                        + " parameter is unsupported: " + name);
            }
        }
    }

    /**
     * 中文说明：执行 validateNode 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate node operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.validateNode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param value 参数 值；parameter value。
     * @param root 参数 root；parameter root。
     * @param protocol 参数 protocol；parameter protocol。
     * @param mcp 参数 MCP；parameter mcp。
     * @param state 参数 state；parameter state。
     */
    private void validateNode(
            String identity,
            Object value,
            Map<String, Object> root,
            String protocol,
            boolean mcp,
            GatewaySchemaValidationState state) {
        if (!(value instanceof Map<?, ?> raw)) {
            return;
        }
        if (++state.nodes > MAX_NODES) {
            throw invalid(identity, "schema node limit exceeded");
        }
        if (state.depth > MAX_DEPTH) {
            throw invalid(identity, "schema depth limit exceeded");
        }
        Map<String, Object> node = cast(raw);
        Object reference = node.get("$ref");
        if (reference != null) {
            if (!(reference instanceof String ref)
                    || !ref.startsWith("#/$defs/")) {
                throw invalid(identity, "external $ref is not allowed");
            }
            String key = ref.substring("#/$defs/".length());
            Map<String, Object> definitions = map(root.get("$defs"));
            if (definitions == null || key.isBlank() || !definitions.containsKey(key)) {
                throw invalid(identity, "unresolved local $ref: " + ref);
            }
        }
        if (node.containsKey("required")) {
            Map<String, Object> properties = map(node.get("properties"));
            if (properties == null) {
                throw invalid(identity, "required requires properties");
            }
            requiredNames(identity, node, properties.keySet());
        }
        Map<String, Object> properties = map(node.get("properties"));
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                validateNode(identity + ".properties." + entry.getKey(),
                        entry.getValue(), root, protocol, mcp,
                        state.child());
            }
        }
        validateChild(identity, "items", node.get("items"), root, protocol, mcp,
                state);
        validateChild(identity, "additionalProperties",
                node.get("additionalProperties"), root, protocol, mcp, state);
        for (String keyword : List.of("anyOf", "oneOf", "allOf", "prefixItems")) {
            Object children = node.get(keyword);
            if (children instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    validateNode(identity + "." + keyword + "[" + i + "]",
                            list.get(i), root, protocol, mcp, state.child());
                }
            } else if (children != null) {
                throw invalid(identity, keyword + " must be an array");
            }
        }
        if (node.get("$defs") instanceof Map<?, ?> definitions) {
            for (Map.Entry<?, ?> entry : definitions.entrySet()) {
                validateNode(identity + ".$defs." + entry.getKey(), entry.getValue(),
                        root, protocol, mcp, state.child());
            }
        }
    }

    /**
     * 中文说明：执行 validateChild 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate child operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.validateChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param keyword 参数 keyword；parameter keyword。
     * @param child 参数 child；parameter child。
     * @param root 参数 root；parameter root。
     * @param protocol 参数 protocol；parameter protocol。
     * @param mcp 参数 MCP；parameter mcp。
     * @param state 参数 state；parameter state。
     */
    private void validateChild(
            String identity,
            String keyword,
            Object child,
            Map<String, Object> root,
            String protocol,
            boolean mcp,
            GatewaySchemaValidationState state) {
        if (child instanceof Boolean) {
            return;
        }
        if (child != null && !(child instanceof Map<?, ?>)) {
            throw invalid(identity, keyword + " must be a schema");
        }
        validateNode(identity + "." + keyword, child, root, protocol, mcp,
                state.child());
    }

    /**
     * 中文说明：执行 validateObjectRoot 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate object root operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.validateObjectRoot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param schema 参数 模式；parameter schema。
     */
    private void validateObjectRoot(String identity, Map<String, Object> schema) {
        if (!"object".equals(schema.get("type"))) {
            throw invalid(identity, "schema root must be an object");
        }
        if (map(schema.get("properties")) == null) {
            throw invalid(identity, "schema properties are required");
        }
    }

    /**
     * 中文说明：执行 requiredNames 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required names operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.requiredNames(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param schema 参数 模式；parameter schema。
     * @param propertyNames 参数 propertyNames；parameter property names。
     * @return 返回 requiredNames 的处理结果；returns the result of the operation.
     */
    private Set<String> requiredNames(
            String identity,
            Map<String, Object> schema,
            Set<String> propertyNames) {
        Object value = schema.get("required");
        if (!(value instanceof List<?> required)) {
            throw invalid(identity, "required must be an array of strings");
        }
        Set<String> result = new HashSet<>();
        for (Object entry : required) {
            if (!(entry instanceof String name) || name.isBlank()
                    || !propertyNames.contains(name) || !result.add(name)) {
                throw invalid(identity, "required contains an unknown or duplicate property");
            }
        }
        return result;
    }

    /**
     * 中文说明：执行 validateRequired 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate required operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.validateRequired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param schema 参数 模式；parameter schema。
     * @param propertyNames 参数 propertyNames；parameter property names。
     */
    private void validateRequired(
            String identity,
            Map<String, Object> schema,
            Set<String> propertyNames) {
        if (schema.containsKey("required")) {
            requiredNames(identity, schema, propertyNames);
        }
    }

    /**
     * 中文说明：执行 registeredForMCP 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the registered for mcp operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.registeredForMcp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 registeredForMCP 的处理结果；returns the result of the operation.
     */
    private boolean registeredForMcp(Map<String, Object> attributes) {
        if (attributes == null) {
            return false;
        }
        Object value = attributes.get("mcpExposure");
        return value instanceof Map<?, ?> exposure
                && Boolean.TRUE.equals(exposure.get("registerMcp"));
    }

    /**
     * 中文说明：执行 map 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?>
                ? (Map<String, Object>) value : null;
    }

    /**
     * 中文说明：执行 cast 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cast operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.cast(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 cast 的处理结果；returns the result of the operation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param message 参数 消息；parameter message。
     * @return 返回 invalid 的处理结果；returns the result of the operation.
     */
    private IllegalArgumentException invalid(String identity, String message) {
        return new IllegalArgumentException(identity + ": " + message);
    }

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code GatewayOperationSchemaValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code GatewayOperationSchemaValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOperationSchemaValidator.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param message 参数 消息；parameter message。
     * @param cause 参数 cause；parameter cause。
     * @return 返回 invalid 的处理结果；returns the result of the operation.
     */
    private IllegalArgumentException invalid(
            String identity,
            String message,
            Throwable cause) {
        return new IllegalArgumentException(identity + ": " + message, cause);
    }


}
