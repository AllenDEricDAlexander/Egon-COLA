package top.egon.cola.component.gateway.admin.catalog.domain.dto;


import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayManualDefinitionRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual定义请求相关的职责与边界。
 * English summary: {@code GatewayManualDefinitionRequestDTO} is an immutable data carrier in the current Gateway module; it owns the manual definition request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param summary 参数 summary；parameter summary。
 * @param tags 参数 tags；parameter tags。
 * @param requestSchema 参数 请求模式；parameter request schema。
 * @param responseSchema 参数 响应模式；parameter response schema。
 * @param errorSchema 参数 error模式；parameter error schema。
 * @param descriptorSnapshot 参数 descriptorSnapshot；parameter descriptor snapshot。
 * @param attributes 参数 attributes；parameter attributes。
 * @param externalAccessible 参数 externalAccessible；parameter external accessible。
 */
public record GatewayManualDefinitionRequestDTO(
        /**
         * 中文说明：保存 summary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by summary; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String summary,
        /**
         * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code List<String>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull List<String> tags,
        /**
         * 中文说明：保存 请求模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by request schema; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull Map<String, Object> requestSchema,
        /**
         * 中文说明：保存 响应模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by response schema; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull Map<String, Object> responseSchema,
        /**
         * 中文说明：保存 error模式 对应的状态、依赖或配置值；字段类型为 {@code List<Map<String, Object>>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error schema; its type is {@code List<Map<String, Object>>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull List<Map<String, Object>> errorSchema,
        /**
         * 中文说明：保存 descriptorSnapshot 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by descriptor snapshot; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> descriptorSnapshot,
        /**
         * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull Map<String, Object> attributes,
        /**
         * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean externalAccessible
) {

    /**
     * 中文说明：执行 定义 操作；该方法是 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the definition operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO.definition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 定义 的处理结果；returns the result of the operation.
     */
    public top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionDTO definition() {
        return new top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionDTO(
                summary,
                tags,
                requestSchema,
                responseSchema,
                errorSchema,
                descriptorSnapshot,
                attributes,
                externalAccessible
        );
    }
}
