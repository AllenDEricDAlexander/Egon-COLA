package top.egon.cola.component.gateway.admin.application.release;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayReleasePublicationStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关发布Publication存储相关的职责与边界。
 * English summary: {@code GatewayReleasePublicationStore} is an interface contract in the current Gateway module; it owns the gateway release publication store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayReleasePublicationStore {

    /**
     * 中文说明：执行 insertAll 操作；该方法是 {@code GatewayReleasePublicationStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert all operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.insertAll(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operations 参数 operations；parameter operations。
     */
    void insertAll(List<PublicationRecord> operations);

    /**
     * 中文说明：执行 findAttempt 操作；该方法是 {@code GatewayReleasePublicationStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find attempt operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.findAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 findAttempt 的处理结果；returns the result of the operation.
     */
    List<PublicationRecord> findAttempt(String releaseId, int attemptNo);

    /**
     * 中文说明：执行 nextIncomplete 操作；该方法是 {@code GatewayReleasePublicationStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the next incomplete operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.nextIncomplete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 nextIncomplete 的处理结果；returns the result of the operation.
     */
    Optional<PublicationRecord> nextIncomplete(
            String releaseId,
            int attemptNo);

    /**
     * 中文说明：执行 findChunkCleanupCandidates 操作；该方法是 {@code GatewayReleasePublicationStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find chunk cleanup candidates operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.findChunkCleanupCandidates(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param successorActivatedBefore 参数 successorActivatedBefore；parameter successor activated before。
     * @return 返回 findChunkCleanupCandidates 的处理结果；returns the result of the operation.
     */
    List<ChunkCleanupCandidate> findChunkCleanupCandidates(
            Instant successorActivatedBefore);

    /**
     * 中文说明：执行 resolveDocument 操作；该方法是 {@code GatewayReleasePublicationStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve document operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.resolveDocument(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 markSubmitted 操作；该方法是 {@code GatewayReleasePublicationStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark submitted operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.markSubmitted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param now 参数 now；parameter now。
     */
    void markSubmitted(String changeId, Instant now);

    /**
     * 中文说明：执行 markResult 操作；该方法是 {@code GatewayReleasePublicationStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark result operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.markResult(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
            PublicationStatus status,
            String errorCode,
            String errorMessage,
            Instant now);

    /**
     * 中文说明：执行 markChunkCleaned 操作；该方法是 {@code GatewayReleasePublicationStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark chunk cleaned operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.markChunkCleaned(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @param now 参数 now；parameter now。
     */
    void markChunkCleaned(String changeId, Instant now);

    /**
     * 中文说明：{@code PublicationRecord} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责PublicationRecord相关的职责与边界。
     * English summary: {@code PublicationRecord} is an immutable data carrier in the current Gateway module; it owns the publication record-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param phaseOrder 参数 phaseOrder；parameter phase order。
     * @param phaseType 参数 phaseType；parameter phase type。
     * @param configKey 参数 config键；parameter config key。
     * @param contentValue 参数 content值；parameter content value。
     * @param contentSha256 参数 contentSha256；parameter content sha256。
     * @param expectedVersion 参数 expectedVersion；parameter expected version。
     * @param changeId 参数 changeId；parameter change id。
     * @param ddcTargetVersion 参数 ddcTargetVersion；parameter ddc target version。
     * @param status 参数 status；parameter status。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param errorMessage 参数 error消息；parameter error message。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    record PublicationRecord(
            /**
             * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String releaseId,
            /**
             * 中文说明：保存 attemptNo 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attempt no; its type is {@code int}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            int attemptNo,
            /**
             * 中文说明：保存 phaseOrder 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by phase order; its type is {@code int}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            int phaseOrder,
            /**
             * 中文说明：保存 phaseType 对应的状态、依赖或配置值；字段类型为 {@code PhaseType}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by phase type; its type is {@code PhaseType}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            PhaseType phaseType,
            /**
             * 中文说明：保存 config键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by config key; its type is {@code String}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String configKey,
            /**
             * 中文说明：保存 content值 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content value; its type is {@code String}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String contentValue,
            /**
             * 中文说明：保存 contentSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content sha256; its type is {@code String}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String contentSha256,
            /**
             * 中文说明：保存 expectedVersion 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected version; its type is {@code Long}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Long expectedVersion,
            /**
             * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeId,
            /**
             * 中文说明：保存 ddcTargetVersion 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by ddc target version; its type is {@code Long}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Long ddcTargetVersion,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code PublicationStatus}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code PublicationStatus}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            PublicationStatus status,
            /**
             * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String errorCode,
            /**
             * 中文说明：保存 error消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error message; its type is {@code String}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String errorMessage,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleasePublicationStore.PublicationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayReleasePublicationStore.PublicationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt
    ) {
    }

    /**
     * 中文说明：{@code PhaseType} 是枚举类型，位于当前 Gateway 模块的相关包中，负责PhaseType相关的职责与边界。
     * English summary: {@code PhaseType} is an enumeration in the current Gateway module; it owns the phase type-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    enum PhaseType {
        /**
         * 中文说明：表示 CHUNK 这一固定值；它属于 {@code GatewayReleasePublicationStore.PhaseType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value chunk; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PhaseType} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PhaseType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PhaseType}; do not couple callers to its representation when the owning type exposes an API.
         */
        CHUNK,
        /**
         * 中文说明：表示 ACTIVATION 这一固定值；它属于 {@code GatewayReleasePublicationStore.PhaseType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value activation; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PhaseType} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PhaseType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PhaseType}; do not couple callers to its representation when the owning type exposes an API.
         */
        ACTIVATION
    }

    /**
     * 中文说明：{@code PublicationStatus} 是枚举类型，位于当前 Gateway 模块的相关包中，负责PublicationStatus相关的职责与边界。
     * English summary: {@code PublicationStatus} is an enumeration in the current Gateway module; it owns the publication status-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    enum PublicationStatus {
        /**
         * 中文说明：表示 PLANNED 这一固定值；它属于 {@code GatewayReleasePublicationStore.PublicationStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value planned; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PublicationStatus} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        PLANNED,
        /**
         * 中文说明：表示 RESOLVED 这一固定值；它属于 {@code GatewayReleasePublicationStore.PublicationStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value resolved; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PublicationStatus} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        RESOLVED,
        /**
         * 中文说明：表示 SUBMITTED 这一固定值；它属于 {@code GatewayReleasePublicationStore.PublicationStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value submitted; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PublicationStatus} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        SUBMITTED,
        /**
         * 中文说明：表示 SUCCESS 这一固定值；它属于 {@code GatewayReleasePublicationStore.PublicationStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value success; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PublicationStatus} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        SUCCESS,
        /**
         * 中文说明：表示 FAILED 这一固定值；它属于 {@code GatewayReleasePublicationStore.PublicationStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value failed; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PublicationStatus} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        FAILED,
        /**
         * 中文说明：表示 PARTIALSUCCESS 这一固定值；它属于 {@code GatewayReleasePublicationStore.PublicationStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value partial success; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PublicationStatus} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        PARTIAL_SUCCESS,
        /**
         * 中文说明：表示 超时 这一固定值；它属于 {@code GatewayReleasePublicationStore.PublicationStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value timeout; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PublicationStatus} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        TIMEOUT,
        /**
         * 中文说明：表示 UNKNOWN 这一固定值；它属于 {@code GatewayReleasePublicationStore.PublicationStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value unknown; it is a state, type, or protocol value of {@code GatewayReleasePublicationStore.PublicationStatus} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.PublicationStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.PublicationStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        UNKNOWN;

        /**
         * 中文说明：执行 terminalResult 操作；该方法是 {@code GatewayReleasePublicationStore.PublicationStatus} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the terminal result operation; this method is the invocation entry point on {@code GatewayReleasePublicationStore.PublicationStatus} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleasePublicationStore.PublicationStatus.terminalResult(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 terminalResult 的处理结果；returns the result of the operation.
         */
        public boolean terminalResult() {
            return switch (this) {
                case SUCCESS, FAILED, PARTIAL_SUCCESS, TIMEOUT, UNKNOWN ->
                        true;
                case PLANNED, RESOLVED, SUBMITTED -> false;
            };
        }
    }

    /**
     * 中文说明：{@code ChunkCleanupCandidate} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ChunkCleanupCandidate相关的职责与边界。
     * English summary: {@code ChunkCleanupCandidate} is an immutable data carrier in the current Gateway module; it owns the chunk cleanup candidate-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param changeId 参数 changeId；parameter change id。
     * @param releaseId 参数 发布Id；parameter release id。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param configKey 参数 config键；parameter config key。
     * @param targetVersion 参数 targetVersion；parameter target version。
     */
    record ChunkCleanupCandidate(
            /**
             * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.ChunkCleanupCandidate}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeId,
            /**
             * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.ChunkCleanupCandidate}; do not couple callers to its representation when the owning type exposes an API.
             */
            String releaseId,
            /**
             * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.ChunkCleanupCandidate}; do not couple callers to its representation when the owning type exposes an API.
             */
            String appCode,
            /**
             * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.ChunkCleanupCandidate}; do not couple callers to its representation when the owning type exposes an API.
             */
            String env,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.ChunkCleanupCandidate}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace,
            /**
             * 中文说明：保存 config键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by config key; its type is {@code String}, and {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.ChunkCleanupCandidate}; do not couple callers to its representation when the owning type exposes an API.
             */
            String configKey,
            /**
             * 中文说明：保存 targetVersion 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by target version; its type is {@code long}, and {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleasePublicationStore.ChunkCleanupCandidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleasePublicationStore.ChunkCleanupCandidate}; do not couple callers to its representation when the owning type exposes an API.
             */
            long targetVersion
    ) {
    }
}
