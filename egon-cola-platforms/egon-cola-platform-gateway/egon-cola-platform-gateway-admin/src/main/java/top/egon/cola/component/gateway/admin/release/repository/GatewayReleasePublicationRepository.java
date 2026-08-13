package top.egon.cola.component.gateway.admin.release.repository;


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
import java.time.Instant;
import java.util.List;
import java.util.Optional;


import top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum;
import top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO;
/**
 * 中文说明：{@code GatewayReleasePublicationRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关发布Publication存储相关的职责与边界。
 * English summary: {@code GatewayReleasePublicationRepository} is an interface contract in the current Gateway module; it owns the gateway release publication store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayReleasePublicationRepository {

    /**
     * 中文说明：执行 insertAll 操作；该方法是 {@code GatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert all operation; this method is the invocation entry point on {@code GatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationRepository.insertAll(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operations 参数 operations；parameter operations。
     */
    void insertAll(List<GatewayReleasePublicationPO> operations);

    /**
     * 中文说明：执行 findAttempt 操作；该方法是 {@code GatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find attempt operation; this method is the invocation entry point on {@code GatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationRepository.findAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 findAttempt 的处理结果；returns the result of the operation.
     */
    List<GatewayReleasePublicationPO> findAttempt(String releaseId, int attemptNo);

    /**
     * 中文说明：执行 nextIncomplete 操作；该方法是 {@code GatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the next incomplete operation; this method is the invocation entry point on {@code GatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationRepository.nextIncomplete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 nextIncomplete 的处理结果；returns the result of the operation.
     */
    Optional<GatewayReleasePublicationPO> nextIncomplete(
            String releaseId,
            int attemptNo);

    /**
     * 中文说明：执行 findChunkCleanupCandidates 操作；该方法是 {@code GatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find chunk cleanup candidates operation; this method is the invocation entry point on {@code GatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationRepository.findChunkCleanupCandidates(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param successorActivatedBefore 参数 successorActivatedBefore；parameter successor activated before。
     * @return 返回 findChunkCleanupCandidates 的处理结果；returns the result of the operation.
     */
    List<GatewayChunkCleanupCandidatePO> findChunkCleanupCandidates(
            Instant successorActivatedBefore);

    /**
     * 中文说明：执行 resolveDocument 操作；该方法是 {@code GatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve document operation; this method is the invocation entry point on {@code GatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationRepository.resolveDocument(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param expectedVersion 参数 expectedVersion；parameter expected version。
     * @param documentContent 参数 documentContent；parameter document content。
     * @param now 参数 now；parameter now。
     */
    void resolveDocument(
            String changeId,
            long expectedVersion,
            String documentContent,
            Instant now);

    /**
     * 中文说明：执行 markSubmitted 操作；该方法是 {@code GatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark submitted operation; this method is the invocation entry point on {@code GatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationRepository.markSubmitted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param now 参数 now；parameter now。
     */
    void markSubmitted(String changeId, Instant now);

    /**
     * 中文说明：执行 markResult 操作；该方法是 {@code GatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark result operation; this method is the invocation entry point on {@code GatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationRepository.markResult(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param targetVersion 参数 targetVersion；parameter target version。
     * @param status 参数 status；parameter status。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param errorMessage 参数 error消息；parameter error message。
     * @param now 参数 now；parameter now。
     */
    void markResult(
            String changeId,
            Long targetVersion,
            GatewayPublicationStatusEnum status,
            String errorCode,
            String errorMessage,
            Instant now);

    /**
     * 中文说明：执行 markChunkCleaned 操作；该方法是 {@code GatewayReleasePublicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark chunk cleaned operation; this method is the invocation entry point on {@code GatewayReleasePublicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationRepository.markChunkCleaned(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param now 参数 now；parameter now。
     */
    void markChunkCleaned(String changeId, Instant now);








}
