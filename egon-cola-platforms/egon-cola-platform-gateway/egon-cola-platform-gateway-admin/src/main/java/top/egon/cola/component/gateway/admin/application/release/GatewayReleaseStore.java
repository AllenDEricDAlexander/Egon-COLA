package top.egon.cola.component.gateway.admin.application.release;

import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayReleaseStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关发布存储相关的职责与边界。
 * English summary: {@code GatewayReleaseStore} is an interface contract in the current Gateway module; it owns the gateway release store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayReleaseStore {

    /**
     * 中文说明：执行 insert 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.insert(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param release 参数 发布；parameter release。
     * @param compiled 参数 compiled；parameter compiled。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     */
    void insert(
            ReleaseRecord release,
            CompiledGatewayRelease compiled,
            int attemptNo);

    /**
     * 中文说明：执行 find 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    Optional<ReleaseRecord> find(String releaseId);

    /**
     * 中文说明：执行 history 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the history operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.history(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 history 的处理结果；returns the result of the operation.
     */
    List<ReleaseRecord> history(String gatewayGroupId);

    /**
     * 中文说明：执行 recoverable 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the recoverable operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.recoverable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 recoverable 的处理结果；returns the result of the operation.
     */
    List<RecoverableAttempt> recoverable();

    /**
     * 中文说明：执行 attempts 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attempts operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.attempts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 attempts 的处理结果；returns the result of the operation.
     */
    List<AttemptRecord> attempts(String releaseId);

    /**
     * 中文说明：执行 latestAttempt 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the latest attempt operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.latestAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 latestAttempt 的处理结果；returns the result of the operation.
     */
    int latestAttempt(String releaseId);

    /**
     * 中文说明：执行 loadCompiled 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load compiled operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.loadCompiled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 loadCompiled 的处理结果；returns the result of the operation.
     */
    CompiledGatewayRelease loadCompiled(String releaseId);

    /**
     * 中文说明：执行 nextAttempt 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the next attempt operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.nextAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param now 参数 now；parameter now。
     * @return 返回 nextAttempt 的处理结果；returns the result of the operation.
     */
    int nextAttempt(String releaseId, Instant now);

    /**
     * 中文说明：执行 beginAttempt 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the begin attempt operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.beginAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param now 参数 now；parameter now。
     */
    void beginAttempt(String releaseId, int attemptNo, Instant now);

    /**
     * 中文说明：执行 completeAttempt 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete attempt operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.completeAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
            List<TargetRecord> targets,
            Instant now);

    /**
     * 中文说明：执行 has发布InProgress 操作；该方法是 {@code GatewayReleaseStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the has release in progress operation; this method is the invocation entry point on {@code GatewayReleaseStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseStore.hasReleaseInProgress(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 has发布InProgress 的处理结果；returns the result of the operation.
     */
    boolean hasReleaseInProgress(String gatewayGroupId);

    /**
     * 中文说明：{@code ReleaseRecord} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责发布Record相关的职责与边界。
     * English summary: {@code ReleaseRecord} is an immutable data carrier in the current Gateway module; it owns the release record-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param draftRevision 参数 草稿Revision；parameter draft revision。
     * @param basedOnReleaseId 参数 basedOn发布Id；parameter based on release id。
     * @param rollbackOfReleaseId 参数 rollbackOf发布Id；parameter rollback of release id。
     * @param status 参数 status；parameter status。
     * @param partialApplied 参数 partialApplied；parameter partial applied。
     * @param changeId 参数 changeId；parameter change id。
     * @param validationReport 参数 validation报告；parameter validation report。
     * @param structuredDiff 参数 structuredDiff；parameter structured diff。
     * @param changeReason 参数 changeReason；parameter change reason。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param createdBy 参数 createdBy；parameter created by。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    record ReleaseRecord(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by draft revision; its type is {@code long}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            long draftRevision,
            /**
             * 中文说明：保存 basedOn发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by based on release id; its type is {@code String}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String basedOnReleaseId,
            /**
             * 中文说明：保存 rollbackOf发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by rollback of release id; its type is {@code String}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String rollbackOfReleaseId,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseStatus}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code GatewayReleaseStatus}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayReleaseStatus status,
            /**
             * 中文说明：保存 partialApplied 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by partial applied; its type is {@code boolean}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean partialApplied,
            /**
             * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeId,
            /**
             * 中文说明：保存 validation报告 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by validation report; its type is {@code Map<String, Object>}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> validationReport,
            /**
             * 中文说明：保存 structuredDiff 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by structured diff; its type is {@code Map<String, Object>}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> structuredDiff,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 createdBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created by; its type is {@code String}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String createdBy,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleaseStore.ReleaseRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayReleaseStore.ReleaseRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.ReleaseRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.ReleaseRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt
    ) {
    }

    /**
     * 中文说明：{@code TargetRecord} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责TargetRecord相关的职责与边界。
     * English summary: {@code TargetRecord} is an immutable data carrier in the current Gateway module; it owns the target record-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param instanceId 参数 instanceId；parameter instance id。
     * @param leaseId 参数 租约Id；parameter lease id。
     * @param status 参数 status；parameter status。
     * @param appliedVersion 参数 appliedVersion；parameter applied version。
     * @param appliedArtifactSha256 参数 applied制品Sha256；parameter applied artifact sha256。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param observedAt 参数 observedAt；parameter observed at。
     */
    record TargetRecord(
            /**
             * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.TargetRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code GatewayReleaseStore.TargetRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.TargetRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.TargetRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String instanceId,
            /**
             * 中文说明：保存 租约Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.TargetRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by lease id; its type is {@code String}, and {@code GatewayReleaseStore.TargetRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.TargetRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.TargetRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String leaseId,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.TargetRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayReleaseStore.TargetRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.TargetRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.TargetRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 appliedVersion 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code GatewayReleaseStore.TargetRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by applied version; its type is {@code Long}, and {@code GatewayReleaseStore.TargetRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.TargetRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.TargetRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Long appliedVersion,
            /**
             * 中文说明：保存 applied制品Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.TargetRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by applied artifact sha256; its type is {@code String}, and {@code GatewayReleaseStore.TargetRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.TargetRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.TargetRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String appliedArtifactSha256,
            /**
             * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.TargetRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code GatewayReleaseStore.TargetRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.TargetRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.TargetRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String errorCode,
            /**
             * 中文说明：保存 observedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleaseStore.TargetRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by observed at; its type is {@code Instant}, and {@code GatewayReleaseStore.TargetRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.TargetRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.TargetRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant observedAt
    ) {
    }

    /**
     * 中文说明：{@code AttemptRecord} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责AttemptRecord相关的职责与边界。
     * English summary: {@code AttemptRecord} is an immutable data carrier in the current Gateway module; it owns the attempt record-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param status 参数 status；parameter status。
     * @param changeId 参数 changeId；parameter change id。
     * @param startedAt 参数 startedAt；parameter started at。
     * @param completedAt 参数 completedAt；parameter completed at。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param errorMessage 参数 error消息；parameter error message。
     * @param targets 参数 targets；parameter targets。
     */
    record AttemptRecord(
            /**
             * 中文说明：保存 attemptNo 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayReleaseStore.AttemptRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attempt no; its type is {@code int}, and {@code GatewayReleaseStore.AttemptRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.AttemptRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.AttemptRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            int attemptNo,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.AttemptRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayReleaseStore.AttemptRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.AttemptRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.AttemptRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.AttemptRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code GatewayReleaseStore.AttemptRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.AttemptRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.AttemptRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeId,
            /**
             * 中文说明：保存 startedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleaseStore.AttemptRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by started at; its type is {@code Instant}, and {@code GatewayReleaseStore.AttemptRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.AttemptRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.AttemptRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant startedAt,
            /**
             * 中文说明：保存 completedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleaseStore.AttemptRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by completed at; its type is {@code Instant}, and {@code GatewayReleaseStore.AttemptRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.AttemptRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.AttemptRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant completedAt,
            /**
             * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.AttemptRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code GatewayReleaseStore.AttemptRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.AttemptRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.AttemptRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String errorCode,
            /**
             * 中文说明：保存 error消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.AttemptRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error message; its type is {@code String}, and {@code GatewayReleaseStore.AttemptRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.AttemptRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.AttemptRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String errorMessage,
            /**
             * 中文说明：保存 targets 对应的状态、依赖或配置值；字段类型为 {@code List<TargetRecord>}，由 {@code GatewayReleaseStore.AttemptRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by targets; its type is {@code List<TargetRecord>}, and {@code GatewayReleaseStore.AttemptRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.AttemptRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.AttemptRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<TargetRecord> targets
    ) {
    }

    /**
     * 中文说明：{@code RecoverableAttempt} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责RecoverableAttempt相关的职责与边界。
     * English summary: {@code RecoverableAttempt} is an immutable data carrier in the current Gateway module; it owns the recoverable attempt-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     */
    record RecoverableAttempt(
            /**
             * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.RecoverableAttempt} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code GatewayReleaseStore.RecoverableAttempt} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.RecoverableAttempt} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.RecoverableAttempt}; do not couple callers to its representation when the owning type exposes an API.
             */
            String releaseId,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseStore.RecoverableAttempt} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayReleaseStore.RecoverableAttempt} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.RecoverableAttempt} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.RecoverableAttempt}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 attemptNo 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayReleaseStore.RecoverableAttempt} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attempt no; its type is {@code int}, and {@code GatewayReleaseStore.RecoverableAttempt} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseStore.RecoverableAttempt} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseStore.RecoverableAttempt}; do not couple callers to its representation when the owning type exposes an API.
             */
            int attemptNo
    ) {
    }
}
