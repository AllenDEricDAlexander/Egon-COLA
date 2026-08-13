package top.egon.cola.component.gateway.admin.rule.domain.dto;


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
import java.time.Duration;
import java.util.UUID;

/**
 * 中文说明：{@code GatewayDdcPublicationCommand} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关DdcPublicationCommand相关的职责与边界。
 * English summary: {@code GatewayDdcPublicationCommand} is an immutable data carrier in the current Gateway module; it owns the gateway ddc publication command-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param bizCode 参数 bizCode；parameter biz code。
 * @param env 参数 env；parameter env。
 * @param appCode 参数 appCode；parameter app code。
 * @param configKey 参数 config键；parameter config key。
 * @param value 参数 值；parameter value。
 * @param expectedVersion 参数 expectedVersion；parameter expected version。
 * @param changeId 参数 changeId；parameter change id。
 * @param operator 参数 operator；parameter operator。
 * @param timeout 参数 超时；parameter timeout。
 */
public record GatewayDdcPublicationCommand(
        /**
         * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bizCode,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode,
        /**
         * 中文说明：保存 config键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by config key; its type is {@code String}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        String configKey,
        /**
         * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code String}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        String value,
        /**
         * 中文说明：保存 expectedVersion 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected version; its type is {@code Long}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long expectedVersion,
        /**
         * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeId,
        /**
         * 中文说明：保存 operator 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operator; its type is {@code String}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operator,
        /**
         * 中文说明：保存 超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayDdcPublicationCommand} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by timeout; its type is {@code Duration}, and {@code GatewayDdcPublicationCommand} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayDdcPublicationCommand} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcPublicationCommand}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration timeout
) {

    /**
     * 中文说明：创建 {@code GatewayDdcPublicationCommand} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDdcPublicationCommand} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param env 参数 env；parameter env。
     * @param appCode 参数 appCode；parameter app code。
     * @param configKey 参数 config键；parameter config key。
     * @param value 参数 值；parameter value。
     * @param expectedVersion 参数 expectedVersion；parameter expected version。
     * @param changeId 参数 changeId；parameter change id。
     * @param operator 参数 operator；parameter operator。
     * @param timeout 参数 超时；parameter timeout。
     */
    public GatewayDdcPublicationCommand {
        bizCode = required(bizCode, "bizCode");
        env = required(env, "env");
        appCode = required(appCode, "appCode");
        configKey = required(configKey, "configKey");
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException(
                    "expectedVersion must not be null or negative"
            );
        }
        changeId = required(changeId, "changeId");
        requireUuidV7(changeId);
        operator = required(operator, "operator");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayDdcPublicationCommand} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayDdcPublicationCommand} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcPublicationCommand.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param fieldName 参数 fieldName；parameter field name。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
        return value.trim();
    }

    /**
     * 中文说明：执行 requireUuidV7 操作；该方法是 {@code GatewayDdcPublicationCommand} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require uuid v7 operation; this method is the invocation entry point on {@code GatewayDdcPublicationCommand} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcPublicationCommand.requireUuidV7(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     */
    private static void requireUuidV7(String value) {
        try {
            UUID uuid = UUID.fromString(canonicalUuid(value));
            if (uuid.version() != 7) {
                throw new IllegalArgumentException(
                        "changeId must be a UUIDv7"
                );
            }
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().contains("UUIDv7")) {
                throw exception;
            }
            throw new IllegalArgumentException(
                    "changeId must be a UUIDv7",
                    exception
            );
        }
    }

    /**
     * 中文说明：执行 canonicalUuid 操作；该方法是 {@code GatewayDdcPublicationCommand} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the canonical uuid operation; this method is the invocation entry point on {@code GatewayDdcPublicationCommand} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcPublicationCommand.canonicalUuid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 canonicalUuid 的处理结果；returns the result of the operation.
     */
    private static String canonicalUuid(String value) {
        if (value.length() != 32) {
            return value;
        }
        return value.substring(0, 8)
                + "-" + value.substring(8, 12)
                + "-" + value.substring(12, 16)
                + "-" + value.substring(16, 20)
                + "-" + value.substring(20);
    }
}
