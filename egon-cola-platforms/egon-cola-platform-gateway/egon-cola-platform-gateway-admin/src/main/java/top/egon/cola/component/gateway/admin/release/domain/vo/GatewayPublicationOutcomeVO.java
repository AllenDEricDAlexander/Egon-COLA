package top.egon.cola.component.gateway.admin.release.domain.vo;


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
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.format.DdcChecksum;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.gateway.admin.rule.domain.vo.CompiledGatewayRelease;
import top.egon.cola.component.gateway.admin.rule.domain.dto.GatewayDdcPublicationCommand;
import top.egon.cola.component.gateway.admin.rule.service.GatewayDdcRulePublisher;
import top.egon.cola.component.gateway.admin.rule.service.GatewayDdcYamlDocument;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum.ACTIVATION;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum.CHUNK;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.FAILED;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.PARTIAL_SUCCESS;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.PLANNED;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.RESOLVED;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.SUBMITTED;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.SUCCESS;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.TIMEOUT;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.UNKNOWN;

import top.egon.cola.component.gateway.admin.release.domain.dto.GatewayPublicationScopeDTO;

/**
 * 中文说明：{@code GatewayPublicationOutcomeVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责PublicationOutcome相关的职责与边界。
 * English summary: {@code GatewayPublicationOutcomeVO} is an immutable data carrier in the current Gateway module; it owns the publication outcome-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param status 参数 status；parameter status。
 * @param changeId 参数 changeId；parameter change id。
 * @param result 参数 result；parameter result。
 * @param partialApplied 参数 partialApplied；parameter partial applied。
 */
public record GatewayPublicationOutcomeVO(
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum status,
        /**
         * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeId,
        /**
         * 中文说明：保存 result 对应的状态、依赖或配置值；字段类型为 {@code DdcManagementPublishResult}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by result; its type is {@code DdcManagementPublishResult}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        DdcManagementPublishResult result,
        /**
         * 中文说明：保存 partialApplied 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by partial applied; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean partialApplied
) {

    /**
     * 中文说明：执行 successful 操作；该方法是 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the successful operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO.successful(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 successful 的处理结果；returns the result of the operation.
     */
    public boolean successful() {
        return status == SUCCESS;
    }
}
