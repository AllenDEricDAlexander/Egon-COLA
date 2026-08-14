package top.egon.cola.component.gateway.admin.release.repository;


import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO;
import top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO;
import top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePO;
import top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO;
import top.egon.cola.component.gateway.admin.rule.domain.vo.CompiledGatewayRelease;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayReleaseRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关发布存储相关的职责与边界。
 * English summary: {@code GatewayReleaseRepository} is an interface contract in the current Gateway module; it owns the gateway release store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayReleaseRepository {

    /**
     * 中文说明：执行 insert 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.insert(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param release 参数 发布；parameter release。
     * @param compiled 参数 compiled；parameter compiled。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     */
    void insert(
            GatewayReleasePO release,
            CompiledGatewayRelease compiled,
            int attemptNo);

    /**
     * 中文说明：执行 find 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    Optional<GatewayReleasePO> find(String releaseId);

    /**
     * 中文说明：执行 history 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the history operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.history(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 history 的处理结果；returns the result of the operation.
     */
    List<GatewayReleasePO> history(String gatewayGroupId);

    /**
     * 中文说明：执行 recoverable 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the recoverable operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.recoverable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 recoverable 的处理结果；returns the result of the operation.
     */
    List<GatewayRecoverableReleaseAttemptPO> recoverable();

    /**
     * 中文说明：执行 attempts 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attempts operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.attempts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 attempts 的处理结果；returns the result of the operation.
     */
    List<GatewayReleaseAttemptPO> attempts(String releaseId);

    /**
     * 中文说明：执行 latestAttempt 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the latest attempt operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.latestAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 latestAttempt 的处理结果；returns the result of the operation.
     */
    int latestAttempt(String releaseId);

    /**
     * 中文说明：执行 loadCompiled 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load compiled operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.loadCompiled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 loadCompiled 的处理结果；returns the result of the operation.
     */
    CompiledGatewayRelease loadCompiled(String releaseId);

    /**
     * 中文说明：执行 nextAttempt 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the next attempt operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.nextAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param now 参数 now；parameter now。
     * @return 返回 nextAttempt 的处理结果；returns the result of the operation.
     */
    int nextAttempt(String releaseId, Instant now);

    /**
     * 中文说明：执行 beginAttempt 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the begin attempt operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.beginAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param now 参数 now；parameter now。
     */
    void beginAttempt(String releaseId, int attemptNo, Instant now);

    /**
     * 中文说明：执行 completeAttempt 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete attempt operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.completeAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param status 参数 status；parameter status。
     * @param partialApplied 参数 partialApplied；parameter partial applied。
     * @param changeId 参数 changeId；parameter change id。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param errorMessage 参数 error消息；parameter error message。
     * @param targets 参数 targets；parameter targets。
     * @param now 参数 now；parameter now。
     */
    void completeAttempt(
            String releaseId,
            int attemptNo,
            GatewayReleaseStatus status,
            boolean partialApplied,
            String changeId,
            String errorCode,
            String errorMessage,
            List<GatewayReleaseTargetPO> targets,
            Instant now);

    /**
     * 中文说明：执行 has发布InProgress 操作；该方法是 {@code GatewayReleaseRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the has release in progress operation; this method is the invocation entry point on {@code GatewayReleaseRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.hasReleaseInProgress(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 has发布InProgress 的处理结果；returns the result of the operation.
     */
    boolean hasReleaseInProgress(String gatewayGroupId);








}
