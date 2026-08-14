package top.egon.cola.component.gateway.admin.release.service;


import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.ddc.format.DdcChecksum;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.gateway.admin.release.domain.dto.GatewayPublicationScopeDTO;
import top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO;
import top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseArtifactVO;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.gateway.admin.rule.domain.dto.GatewayDdcPublicationCommand;
import top.egon.cola.component.gateway.admin.rule.domain.vo.CompiledGatewayRelease;
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

/**
 * 中文说明：{@code GatewayReleasePublicationCoordinator} 是类型，位于当前 Gateway 模块的相关包中，负责网关发布PublicationCoordinator相关的职责与边界。
 * English summary: {@code GatewayReleasePublicationCoordinator} is a type in the current Gateway module; it owns the gateway release publication coordinator-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayReleasePublicationCoordinator {

    /**
     * 中文说明：保存 journal 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleasePublicationRepository}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by journal; its type is {@code GatewayReleasePublicationRepository}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleasePublicationRepository journal;

    /**
     * 中文说明：保存 releases 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseRepository}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by releases; its type is {@code GatewayReleaseRepository}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleaseRepository releases;

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code DdcManagementClient}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code DdcManagementClient}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcManagementClient client;

    /**
     * 中文说明：保存 发布器 对应的状态、依赖或配置值；字段类型为 {@code GatewayDdcRulePublisher}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by publisher; its type is {@code GatewayDdcRulePublisher}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDdcRulePublisher publisher;

    /**
     * 中文说明：保存 yamlDocument 对应的状态、依赖或配置值；字段类型为 {@code GatewayDdcYamlDocument}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by yaml document; its type is {@code GatewayDdcYamlDocument}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDdcYamlDocument yamlDocument;

    /**
     * 中文说明：保存 targetBizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by target biz code; its type is {@code String}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String targetBizCode;

    /**
     * 中文说明：保存 targetAppCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by target app code; its type is {@code String}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String targetAppCode;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayReleasePublicationCoordinator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by timeout; its type is {@code Duration}, and {@code GatewayReleasePublicationCoordinator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationCoordinator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationCoordinator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration timeout;

    /**
     * 中文说明：创建 {@code GatewayReleasePublicationCoordinator} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReleasePublicationCoordinator} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param journal 参数 journal；parameter journal。
     * @param releases 参数 releases；parameter releases。
     * @param client 参数 客户端；parameter client。
     * @param publisher 参数 发布器；parameter publisher。
     * @param clock 参数 clock；parameter clock。
     * @param timeout 参数 超时；parameter timeout。
     * @param targetBizCode 参数 targetBizCode；parameter target biz code。
     * @param targetAppCode 参数 targetAppCode；parameter target app code。
     */
    public GatewayReleasePublicationCoordinator(
            GatewayReleasePublicationRepository journal,
            GatewayReleaseRepository releases,
            DdcManagementClient client,
            GatewayDdcRulePublisher publisher,
            Clock clock,
            Duration timeout,
            String targetBizCode,
            String targetAppCode) {
        this.journal = Objects.requireNonNull(journal, "journal");
        this.releases = Objects.requireNonNull(releases, "releases");
        this.client = Objects.requireNonNull(client, "client");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.yamlDocument = new GatewayDdcYamlDocument();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.timeout = positive(timeout);
        this.targetBizCode = required(targetBizCode, "targetBizCode");
        this.targetAppCode = required(targetAppCode, "targetAppCode");
    }

    /**
     * 中文说明：执行 resume 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resume operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.resume(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 resume 的处理结果；returns the result of the operation.
     */
    public GatewayPublicationOutcomeVO resume(String releaseId, int attemptNo) {
        return execute(
                releaseId,
                attemptNo,
                releases.loadCompiled(releaseId),
                "gateway_release_reconciler"
        );
    }

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param compiled 参数 compiled；parameter compiled。
     * @param actorId 参数 actorId；parameter actor id。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
    public GatewayPublicationOutcomeVO execute(
            String releaseId,
            int attemptNo,
            CompiledGatewayRelease compiled,
            String actorId) {
        Objects.requireNonNull(compiled, "compiled");
        String operator = required(actorId, "actorId");
        GatewayPublicationScopeDTO scope = scope(compiled);
        List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO> operations =
                initialize(releaseId, attemptNo, compiled);
        if (operations.stream().anyMatch(operation ->
                operation.status() != SUCCESS)) {
            publisher.ensureReadyTarget(
                    scope.bizCode(),
                    scope.env(),
                    scope.appCode()
            );
        }
        int successfulPhases = 0;
        DdcManagementPublishResult latestResult = null;
        for (top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO original
                : operations) {
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation =
                    current(releaseId, attemptNo, original.changeId());
            if (operation.status() == SUCCESS) {
                successfulPhases++;
                continue;
            }
            DdcManagementPublishResult result = execute(
                    scope,
                    operation,
                    operator
            );
            latestResult = result;
            top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum status =
                    status(result.status());
            recordResult(operation.changeId(), result, status);
            if (status != SUCCESS) {
                return new GatewayPublicationOutcomeVO(
                        status,
                        operation.changeId(),
                        result,
                        successfulPhases > 0
                );
            }
            successfulPhases++;
        }
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO activation =
                journal.findAttempt(releaseId, attemptNo).getLast();
        DdcManagementPublishResult result = latestResult != null
                && activation.changeId().equals(latestResult.changeId())
                ? latestResult
                : publishedResult(activation);
        return new GatewayPublicationOutcomeVO(
                SUCCESS,
                activation.changeId(),
                result,
                false
        );
    }

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult execute(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator) {
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO current = operation;
        if (current.status() == PLANNED) {
            current = resolve(scope, current, operator);
        }
        if (current.status() == RESOLVED) {
            journal.markSubmitted(current.changeId(), clock.instant());
            return publish(scope, current, operator);
        }
        if (current.status() == SUBMITTED) {
            return resumeSubmitted(scope, current, operator);
        }
        if (current.status() == FAILED
                || current.status() == PARTIAL_SUCCESS
                || current.status() == TIMEOUT
                || current.status() == UNKNOWN) {
            return retry(scope, current, operator);
        }
        throw new IllegalStateException(
                "publication phase cannot execute from " + current.status()
        );
    }

    /**
     * 中文说明：执行 publish 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.publish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @return 返回 publish 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult publish(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator) {
        try {
            return publisher.publish(command(scope, operation, operator));
        } catch (RuntimeException failure) {
            return recover(operation, failure);
        }
    }

    /**
     * 中文说明：执行 resumeSubmitted 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resume submitted operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.resumeSubmitted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @return 返回 resumeSubmitted 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult resumeSubmitted(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator) {
        try {
            return result(client.getPublishTask(operation.changeId()));
        } catch (DdcManagementClientException exception) {
            if (exception.code()
                    == DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND
                    .getCode()) {
                return republish(scope, operation, operator);
            }
            return unknown(operation, exception);
        } catch (RuntimeException failure) {
            return unknown(operation, failure);
        }
    }

    /**
     * 中文说明：执行 重试 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retry operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.retry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @return 返回 重试 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult retry(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator) {
        try {
            DdcManagementPublishTask task =
                    client.getPublishTask(operation.changeId());
            if (task.status() == DdcManagementPublishStatus.PENDING
                    || task.status()
                    == DdcManagementPublishStatus.PUBLISHING
                    || task.status() == DdcManagementPublishStatus.SUCCESS) {
                return result(task);
            }
            return client.retry(operation.changeId());
        } catch (DdcManagementClientException exception) {
            if (exception.code()
                    == DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND
                    .getCode()) {
                return republish(scope, operation, operator);
            }
            return unknown(operation, exception);
        } catch (RuntimeException failure) {
            return recover(operation, failure);
        }
    }

    /**
     * 中文说明：执行 recover 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the recover operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.recover(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param publishFailure 参数 publishFailure；parameter publish failure。
     * @return 返回 recover 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult recover(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            RuntimeException publishFailure) {
        try {
            return result(client.getPublishTask(operation.changeId()));
        } catch (RuntimeException queryFailure) {
            publishFailure.addSuppressed(queryFailure);
            return unknown(operation, publishFailure);
        }
    }

    /**
     * 中文说明：执行 resolve 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @return 返回 resolve 的处理结果；returns the result of the operation.
     */
    private top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO resolve(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator) {
        return resolve(
                scope,
                operation,
                operator,
                operation.contentValue()
        );
    }

    /**
     * 中文说明：执行 resolve 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @param leafValue 参数 leaf值；parameter leaf value。
     * @return 返回 resolve 的处理结果；returns the result of the operation.
     */
    private top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO resolve(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator,
            String leafValue) {
        DdcManagementConfigQuery query = new DdcManagementConfigQuery(
                scope.bizCode(),
                scope.env(),
                scope.appCode()
        );
        DdcManagementConfig config = client.findConfig(query).orElse(null);
        if (config == null) {
            String initialDocument = yamlDocument.putLeaf(
                    null,
                    operation.configKey(),
                    leafValue
            );
            config = create(
                    scope,
                    operation,
                    operator,
                    query,
                    initialDocument
            );
        }
        validateConfig(config, operation.configKey());
        String documentContent = yamlDocument.putLeaf(
                config.content(),
                operation.configKey(),
                leafValue
        );
        journal.resolveDocument(
                operation.changeId(),
                config.version(),
                documentContent,
                clock.instant()
        );
        return current(
                operation.releaseId(),
                operation.attemptNo(),
                operation.changeId()
        );
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @param query 参数 query；parameter query。
     * @param documentContent 参数 documentContent；parameter document content。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    private DdcManagementConfig create(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator,
            DdcManagementConfigQuery query,
            String documentContent) {
        try {
            return client.upsert(new DdcManagementConfigUpsertRequest(
                    scope.bizCode(),
                    scope.env(),
                    scope.appCode(),
                    GatewayDdcYamlDocument.RESOURCE_NAME,
                    documentContent,
                    GatewayDdcYamlDocument.FORMAT,
                    "Gateway release " + operation.releaseId(),
                    0L,
                    operator
            ));
        } catch (RuntimeException failure) {
            DdcManagementConfig recovered = client.findConfig(query)
                    .orElseThrow(() -> failure);
            validateConfig(recovered, operation.configKey());
            return recovered;
        }
    }

    /**
     * 中文说明：执行 validateConfig 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate config operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.validateConfig(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param config 参数 config；parameter config。
     * @param configKey 参数 config键；parameter config key。
     */
    private void validateConfig(
            DdcManagementConfig config,
            String configKey) {
        if (config.version() == null || config.version() < 0) {
            throw new IllegalStateException(
                    "DDC config has no usable version: " + configKey
            );
        }
        if (config.deleted()) {
            throw new IllegalStateException(
                    "DDC config is deleted: " + configKey
            );
        }
        if (!config.enabled()) {
            throw new IllegalStateException(
                    "DDC config is disabled: " + configKey
            );
        }
        if (!GatewayDdcYamlDocument.RESOURCE_NAME.equals(
                config.resourceName())
                || !GatewayDdcYamlDocument.FORMAT.equals(config.format())) {
            throw new IllegalStateException(
                    "DDC config is not application.yml/YAML: " + configKey
            );
        }
    }

    /**
     * 中文说明：执行 republish 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the republish operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.republish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @return 返回 republish 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult republish(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator) {
        String leafValue = yamlDocument.leafValue(
                operation.contentValue(),
                operation.configKey()
        ).orElseThrow(() -> new IllegalStateException(
                "resolved Gateway rule leaf is missing"
        ));
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO resolved = resolve(
                scope,
                operation,
                operator,
                leafValue
        );
        journal.markSubmitted(resolved.changeId(), clock.instant());
        return publish(scope, resolved, operator);
    }

    /**
     * 中文说明：执行 command 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the command operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.command(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param operation 参数 操作；parameter operation。
     * @param operator 参数 operator；parameter operator。
     * @return 返回 command 的处理结果；returns the result of the operation.
     */
    private GatewayDdcPublicationCommand command(
            GatewayPublicationScopeDTO scope,
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            String operator) {
        return new GatewayDdcPublicationCommand(
                scope.bizCode(),
                scope.env(),
                scope.appCode(),
                operation.configKey(),
                operation.contentValue(),
                operation.expectedVersion(),
                operation.changeId(),
                operator,
                timeout
        );
    }

    /**
     * 中文说明：执行 initialize 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the initialize operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.initialize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param compiled 参数 compiled；parameter compiled。
     * @return 返回 initialize 的处理结果；returns the result of the operation.
     */
    private List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO> initialize(
            String releaseId,
            int attemptNo,
            CompiledGatewayRelease compiled) {
        List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO> existing =
                journal.findAttempt(releaseId, attemptNo);
        List<GatewayReleaseArtifactVO> artifacts = artifacts(compiled);
        if (!existing.isEmpty()) {
            validateExisting(existing, artifacts);
            return existing;
        }
        Instant now = clock.instant();
        List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO> created =
                new ArrayList<>();
        for (int index = 0; index < artifacts.size(); index++) {
            GatewayReleaseArtifactVO artifact = artifacts.get(index);
            created.add(new top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO(
                    required(releaseId, "releaseId"),
                    attemptNo,
                    index,
                    artifact.phaseType(),
                    artifact.configKey(),
                    artifact.value(),
                    checksum(artifact.value()),
                    null,
                    UuidV7.simpleString(),
                    null,
                    PLANNED,
                    null,
                    null,
                    now,
                    now
            ));
        }
        journal.insertAll(created);
        return List.copyOf(created);
    }

    /**
     * 中文说明：执行 artifacts 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the artifacts operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.artifacts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param compiled 参数 compiled；parameter compiled。
     * @return 返回 artifacts 的处理结果；returns the result of the operation.
     */
    private List<GatewayReleaseArtifactVO> artifacts(CompiledGatewayRelease compiled) {
        List<GatewayReleaseArtifactVO> artifacts = new ArrayList<>();
        compiled.activation().chunks().stream()
                .sorted(Comparator.comparingInt(GatewayRuleChunkRef::index))
                .forEach(chunk -> artifacts.add(new GatewayReleaseArtifactVO(
                        CHUNK,
                        chunk.configKey(),
                        required(
                                compiled.chunkValues().get(chunk.configKey()),
                                "chunk value"
                        )
                )));
        artifacts.add(new GatewayReleaseArtifactVO(
                ACTIVATION,
                GatewayDdcRulePublisher.ACTIVE_CONFIG_KEY,
                compiled.activationJson()
        ));
        return List.copyOf(artifacts);
    }

    /**
     * 中文说明：执行 validateExisting 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate existing operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.validateExisting(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param existing 参数 existing；parameter existing。
     * @param artifacts 参数 artifacts；parameter artifacts。
     */
    private void validateExisting(
            List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO> existing,
            List<GatewayReleaseArtifactVO> artifacts) {
        if (existing.size() != artifacts.size()) {
            throw new IllegalStateException(
                    "publication journal does not match compiled release"
            );
        }
        for (int index = 0; index < artifacts.size(); index++) {
            GatewayReleaseArtifactVO artifact = artifacts.get(index);
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation =
                    existing.get(index);
            if (operation.phaseOrder() != index
                    || operation.phaseType() != artifact.phaseType()
                    || !operation.configKey().equals(artifact.configKey())
                    || !operation.contentSha256().equals(
                    checksum(artifact.value()))) {
                throw new IllegalStateException(
                        "publication journal content conflict"
                );
            }
        }
    }

    /**
     * 中文说明：执行 current 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.current(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param changeId 参数 changeId；parameter change id。
     * @return 返回 current 的处理结果；returns the result of the operation.
     */
    private top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO current(
            String releaseId,
            int attemptNo,
            String changeId) {
        return journal.findAttempt(releaseId, attemptNo).stream()
                .filter(operation -> operation.changeId().equals(changeId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "publication operation disappeared"
                ));
    }

    /**
     * 中文说明：执行 recordResult 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record result operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.recordResult(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param result 参数 result；parameter result。
     * @param status 参数 status；parameter status。
     */
    private void recordResult(
            String changeId,
            DdcManagementPublishResult result,
            top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum status) {
        journal.markResult(
                changeId,
                result.targetVersion(),
                status,
                status == SUCCESS ? null : "DDC_PUBLISH_" + status,
                result.errorMessage(),
                clock.instant()
        );
    }

    /**
     * 中文说明：执行 result 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the result operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.result(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @return 返回 result 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult result(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation) {
        return new DdcManagementPublishResult(
                operation.changeId(),
                DdcManagementPublishStatus.SUCCESS,
                operation.ddcTargetVersion(),
                resourceChecksum(operation.contentValue()),
                0,
                List.of(),
                null,
                operation.createdAt(),
                operation.updatedAt(),
                operation.updatedAt()
        );
    }

    /**
     * 中文说明：执行 publishedResult 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the published result operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.publishedResult(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @return 返回 publishedResult 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult publishedResult(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation) {
        try {
            return result(client.getPublishTask(operation.changeId()));
        } catch (RuntimeException unavailable) {
            return result(operation);
        }
    }

    /**
     * 中文说明：执行 result 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the result operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.result(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     * @return 返回 result 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult result(DdcManagementPublishTask task) {
        return new DdcManagementPublishResult(
                task.changeId(),
                task.status(),
                task.targetVersion(),
                task.resourceChecksum(),
                task.targetCount(),
                task.targets(),
                task.errorMessage(),
                task.createdAt(),
                task.dispatchedAt(),
                task.completedAt()
        );
    }

    /**
     * 中文说明：执行 unknown 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unknown operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.unknown(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param failure 参数 failure；parameter failure。
     * @return 返回 unknown 的处理结果；returns the result of the operation.
     */
    private DdcManagementPublishResult unknown(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO operation,
            RuntimeException failure) {
        return new DdcManagementPublishResult(
                operation.changeId(),
                DdcManagementPublishStatus.UNKNOWN,
                operation.ddcTargetVersion(),
                resourceChecksum(operation.contentValue()),
                0,
                List.of(),
                failure.getMessage(),
                operation.createdAt(),
                null,
                clock.instant()
        );
    }

    /**
     * 中文说明：执行 status 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the status operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.status(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 status 的处理结果；returns the result of the operation.
     */
    private top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum status(
            DdcManagementPublishStatus status) {
        return switch (status) {
            case SUCCESS -> SUCCESS;
            case PARTIAL_SUCCESS -> PARTIAL_SUCCESS;
            case FAILED -> FAILED;
            case TIMEOUT -> TIMEOUT;
            case PENDING, PUBLISHING, UNKNOWN -> UNKNOWN;
        };
    }

    /**
     * 中文说明：执行 scope 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the scope operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.scope(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param compiled 参数 compiled；parameter compiled。
     * @return 返回 scope 的处理结果；returns the result of the operation.
     */
    private GatewayPublicationScopeDTO scope(CompiledGatewayRelease compiled) {
        return new GatewayPublicationScopeDTO(
                targetBizCode,
                compiled.snapshot().content().env(),
                targetAppCode
        );
    }

    /**
     * 中文说明：执行 checksum 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the checksum operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.checksum(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 checksum 的处理结果；returns the result of the operation.
     */
    private String checksum(String value) {
        return GatewayRuleCanonicalizer.sha256(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 中文说明：执行 资源Checksum 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resource checksum operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.resourceChecksum(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 资源Checksum 的处理结果；returns the result of the operation.
     */
    private String resourceChecksum(String content) {
        return DdcChecksum.resource(
                GatewayDdcYamlDocument.RESOURCE_NAME,
                GatewayDdcYamlDocument.FORMAT,
                content
        );
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private Duration positive(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        return value;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayReleasePublicationCoordinator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayReleasePublicationCoordinator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationCoordinator.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }






}
