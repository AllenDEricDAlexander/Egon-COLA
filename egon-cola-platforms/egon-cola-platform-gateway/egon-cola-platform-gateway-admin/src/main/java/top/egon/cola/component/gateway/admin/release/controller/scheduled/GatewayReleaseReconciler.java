package top.egon.cola.component.gateway.admin.release.controller.scheduled;


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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTarget;
import top.egon.cola.component.gateway.admin.release.service.GatewayReleasePublicationCoordinator;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;

import java.time.Clock;
import java.util.List;

/**
 * 中文说明：{@code GatewayReleaseReconciler} 是类型，位于当前 Gateway 模块的相关包中，负责网关发布Reconciler相关的职责与边界。
 * English summary: {@code GatewayReleaseReconciler} is a type in the current Gateway module; it owns the gateway release reconciler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Component
public class GatewayReleaseReconciler {

    /**
     * 中文说明：保存 releases 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseRepository}，由 {@code GatewayReleaseReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by releases; its type is {@code GatewayReleaseRepository}, and {@code GatewayReleaseReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleaseRepository releases;

    /**
     * 中文说明：保存 coordinator 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleasePublicationCoordinator}，由 {@code GatewayReleaseReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by coordinator; its type is {@code GatewayReleasePublicationCoordinator}, and {@code GatewayReleaseReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleasePublicationCoordinator coordinator;

    /**
     * 中文说明：保存 drafts 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code GatewayReleaseReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drafts; its type is {@code GatewayDraftRepository}, and {@code GatewayReleaseReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftJpaRepository drafts;

    /**
     * 中文说明：保存 transactions 对应的状态、依赖或配置值；字段类型为 {@code TransactionTemplate}，由 {@code GatewayReleaseReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transactions; its type is {@code TransactionTemplate}, and {@code GatewayReleaseReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final TransactionTemplate transactions;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayReleaseReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayReleaseReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayReleaseReconciler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReleaseReconciler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param releases 参数 releases；parameter releases。
     * @param coordinator 参数 coordinator；parameter coordinator。
     * @param drafts 参数 drafts；parameter drafts。
     * @param transactions 参数 transactions；parameter transactions。
     */
    @Autowired
    public GatewayReleaseReconciler(
            GatewayReleaseRepository releases,
            ObjectProvider<GatewayReleasePublicationCoordinator>
                    coordinator,
            GatewayDraftJpaRepository drafts,
            TransactionTemplate transactions) {
        this(
                releases,
                coordinator.getIfAvailable(),
                drafts,
                transactions,
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayReleaseReconciler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReleaseReconciler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param releases 参数 releases；parameter releases。
     * @param coordinator 参数 coordinator；parameter coordinator。
     * @param drafts 参数 drafts；parameter drafts。
     * @param transactions 参数 transactions；parameter transactions。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayReleaseReconciler(
            GatewayReleaseRepository releases,
            GatewayReleasePublicationCoordinator coordinator,
            GatewayDraftJpaRepository drafts,
            TransactionTemplate transactions,
            Clock clock) {
        this.releases = releases;
        this.coordinator = coordinator;
        this.drafts = drafts;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 reconcile 操作；该方法是 {@code GatewayReleaseReconciler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reconcile operation; this method is the invocation entry point on {@code GatewayReleaseReconciler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseReconciler.reconcile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.release-reconcile-delay:30000}"
    )
    public void reconcile() {
        if (coordinator == null) {
            return;
        }
        releases.recoverable().forEach(this::reconcile);
    }

    /**
     * 中文说明：执行 reconcile 操作；该方法是 {@code GatewayReleaseReconciler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reconcile operation; this method is the invocation entry point on {@code GatewayReleaseReconciler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseReconciler.reconcile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attempt 参数 attempt；parameter attempt。
     */
    private void reconcile(top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO attempt) {
        top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO outcome;
        try {
            outcome = coordinator.resume(
                    attempt.releaseId(),
                    attempt.attemptNo()
            );
        } catch (RuntimeException unavailable) {
            return;
        }
        List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO> targets = outcome.result()
                .targets()
                .stream()
                .map(this::target)
                .toList();
        transactions.executeWithoutResult(transaction -> {
            releases.completeAttempt(
                    attempt.releaseId(),
                    attempt.attemptNo(),
                    releaseStatus(outcome.status()),
                    outcome.partialApplied(),
                    outcome.changeId(),
                    outcome.successful()
                            ? null
                            : "DDC_PUBLISH_" + outcome.status(),
                    outcome.result().errorMessage(),
                    targets,
                    clock.instant()
            );
            if (outcome.successful()) {
                advanceDraft(attempt);
            }
        });
    }

    /**
     * 中文说明：执行 advance草稿 操作；该方法是 {@code GatewayReleaseReconciler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the advance draft operation; this method is the invocation entry point on {@code GatewayReleaseReconciler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseReconciler.advanceDraft(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attempt 参数 attempt；parameter attempt。
     */
    private void advanceDraft(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO attempt) {
        GatewayDraftPO draft = drafts.findById(
                attempt.gatewayGroupId()
        ).orElse(null);
        if (draft != null
                && !attempt.releaseId().equals(
                draft.getBasedOnReleaseId())) {
            draft.baseOn(
                    attempt.releaseId(),
                    "gateway_release_reconciler",
                    clock.instant()
            );
            drafts.flush();
        }
    }

    /**
     * 中文说明：执行 发布Status 操作；该方法是 {@code GatewayReleaseReconciler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the release status operation; this method is the invocation entry point on {@code GatewayReleaseReconciler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseReconciler.releaseStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 发布Status 的处理结果；returns the result of the operation.
     */
    private GatewayReleaseStatus releaseStatus(
            top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum status) {
        return switch (status) {
            case SUCCESS -> GatewayReleaseStatus.SUCCESS;
            case FAILED, PARTIAL_SUCCESS -> GatewayReleaseStatus.FAILED;
            case TIMEOUT -> GatewayReleaseStatus.TIMEOUT;
            case PLANNED, RESOLVED, SUBMITTED, UNKNOWN ->
                    GatewayReleaseStatus.UNKNOWN;
        };
    }

    /**
     * 中文说明：执行 target 操作；该方法是 {@code GatewayReleaseReconciler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the target operation; this method is the invocation entry point on {@code GatewayReleaseReconciler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseReconciler.target(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     * @return 返回 target 的处理结果；returns the result of the operation.
     */
    private top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO target(
            DdcManagementPublishTarget target) {
        return new top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO(
                target.instanceId(),
                target.leaseId(),
                target.status(),
                target.currentVersion(),
                null,
                target.errorMessage() == null
                        ? null
                        : "DDC_TARGET_ERROR",
                target.ackAt() == null
                        ? clock.instant()
                        : target.ackAt()
        );
    }
}
