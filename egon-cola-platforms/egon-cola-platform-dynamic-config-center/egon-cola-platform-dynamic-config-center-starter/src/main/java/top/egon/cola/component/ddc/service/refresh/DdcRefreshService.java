package top.egon.cola.component.ddc.service.refresh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.format.DdcChecksum;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;
import top.egon.cola.component.ddc.model.config.DdcPublishTarget;
import top.egon.cola.component.ddc.model.config.DdcAckStatus;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.service.refresh.DdcYamlConfigApplier;
import top.egon.cola.component.ddc.state.DdcLocalConfigState;
import top.egon.cola.component.ddc.service.lifecycle.DdcAckDelivery;
import top.egon.cola.component.ddc.state.DdcLeaseSessionHolder;

import java.util.List;

/**
 * 协调 DDC YAML 快照初始化、发布消息应用、版本幂等判断和 ACK 反馈。
 * Coordinates DDC YAML snapshot initialization, publication application, version idempotency checks, and ACK feedback.
 */
public class DdcRefreshService {

    /**
     * 当前类的日志记录器。 Logger for this class.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcRefreshService.class);

    /**
     * ACK 错误消息允许的最大字符数。 Maximum character count allowed for ACK error messages.
     */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 256;

    /**
     * 保存本地版本、校验和及配置锁的仓库。 Repository storing local version, checksum, and configuration locks.
     */
    private final DdcLocalConfigState repository;

    /**
     * 执行 YAML 环境更新和运行时刷新的应用器。 Applier performing YAML environment updates and runtime refresh.
     */
    private final DdcYamlConfigApplier yamlConfigApplier;

    /**
     * 提交发布确认的抽象。 Abstraction that submits publication acknowledgments.
     */
    private final AckSubmitter ackSubmitter;

    /**
     * 提供当前实例与租约目标信息的会话持有器。 Session holder providing current instance and lease targeting information.
     */
    private final DdcLeaseSessionHolder sessionHolder;

    /**
     * ConfigData 导入的唯一配置资源名。 Sole configuration resource name imported through ConfigData.
     */
    private final String resourceName;

    /**
     * ConfigData 导入资源的配置格式。 Configuration format of the resource imported through ConfigData.
     */
    private final String format;

    /**
     * 创建使用管理客户端同步发送 ACK 的刷新服务。
     * Creates a refresh service that sends ACKs synchronously through the administration client.
     *
     * @param repository        本地配置仓库; local configuration repository
     * @param yamlConfigApplier YAML 配置应用器; YAML configuration applier
     * @param adminClient       DDC 管理端客户端; DDC administration client
     * @param sessionHolder     租约会话持有器; lease session holder
     */
    public DdcRefreshService(DdcLocalConfigState repository,
                             DdcYamlConfigApplier yamlConfigApplier,
                             DdcConfigClient adminClient,
                             DdcLeaseSessionHolder sessionHolder) {
        this(
                repository,
                yamlConfigApplier,
                directAck(adminClient),
                sessionHolder
        );
    }

    /**
     * 创建使用异步 ACK 投递组件的刷新服务。
     * Creates a refresh service using the asynchronous ACK delivery component.
     *
     * @param repository        本地配置仓库; local configuration repository
     * @param yamlConfigApplier YAML 配置应用器; YAML configuration applier
     * @param ackDelivery       异步 ACK 投递组件; asynchronous ACK delivery component
     * @param sessionHolder     租约会话持有器; lease session holder
     */
    public DdcRefreshService(DdcLocalConfigState repository,
                             DdcYamlConfigApplier yamlConfigApplier,
                             DdcAckDelivery ackDelivery,
                             DdcLeaseSessionHolder sessionHolder) {
        this(
                repository,
                yamlConfigApplier,
                ackDelivery::submit,
                sessionHolder
        );
    }

    /**
     * 创建刷新服务并用 ConfigData 快照初始化缺失的本地元数据。
     * Creates the refresh service and seeds missing local metadata from the ConfigData snapshot.
     *
     * @param repository        本地配置仓库; local configuration repository
     * @param yamlConfigApplier YAML 配置应用器; YAML configuration applier
     * @param ackSubmitter      ACK 提交器; ACK submitter
     * @param sessionHolder     租约会话持有器; lease session holder
     */
    private DdcRefreshService(DdcLocalConfigState repository,
                              DdcYamlConfigApplier yamlConfigApplier,
                              AckSubmitter ackSubmitter,
                              DdcLeaseSessionHolder sessionHolder) {
        this.repository = repository;
        this.yamlConfigApplier = yamlConfigApplier;
        this.ackSubmitter = ackSubmitter;
        this.sessionHolder = sessionHolder;
        DdcDynamicPropertySource.Snapshot snapshot =
                yamlConfigApplier.currentSnapshot();
        this.resourceName = snapshot.resourceName();
        this.format = snapshot.format().name();
        seedConfigDataMetadata();
    }

    /**
     * 应用作用域初始快照；当前作用域必须为空或仅包含一个 {@code application.yml}。
     * Applies initial scope snapshots; the current scope must be empty or contain exactly one {@code application.yml}.
     *
     * @param configs 管理端返回的配置快照; configuration snapshots returned by the administration endpoint
     * @throws IllegalArgumentException 作用域包含多个资源时抛出; thrown when the scope contains multiple resources
     */
    public void applySnapshots(List<DdcConfigValue> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        if (configs.size() != 1) {
            throw new IllegalArgumentException(
                    "DDC scope must contain exactly one YAML resource"
            );
        }
        applySnapshot(configs.getFirst());
    }

    /**
     * 在资源锁内校验并应用比本地版本更新的 YAML 快照。
     * Validates and applies a YAML snapshot newer than the local version under the resource lock.
     *
     * @param config 配置快照; configuration snapshot
     * @throws IllegalArgumentException 快照不是有效 YAML 资源时抛出; thrown when the snapshot is not a valid YAML resource
     */
    public void applySnapshot(DdcConfigValue config) {
        requireYamlResource(config);
        repository.withConfigLock(resourceName, () -> {
            String resourceChecksum =
                    DdcChecksum.resource(
                            resourceName,
                            format,
                            config.getContent()
                    );
            ConfigMetadata local = metadata();
            VersionRelation relation = compare(
                    local,
                    config.getVersion(),
                    resourceChecksum
            );
            if (relation == VersionRelation.CHECKSUM_CONFLICT) {
                LOGGER.warn(
                        "DDC snapshot checksum conflict for resource={} version={}",
                        resourceName,
                        config.getVersion()
                );
            } else if (relation == VersionRelation.NEWER) {
                applyAndStore(
                        config.getContent(),
                        config.getVersion(),
                        resourceChecksum,
                        null,
                        local
                );
            }
            return null;
        });
    }

    /**
     * 校验发布消息和目标租约，在资源锁内应用配置并提交 ACK。
     * Validates a publication message and target lease, applies configuration under the resource lock, and submits an ACK.
     *
     * @param message Redis 发布消息; Redis publication message
     */
    public void refresh(@Nullable DdcPublishMessage message) {
        if (!isValid(message)) {
            return;
        }
        DdcLeaseSession session = sessionHolder.current().orElse(null);
        if (session == null || !isTarget(message, session)) {
            return;
        }

        AckOutcome outcome = repository.withConfigLock(
                resourceName,
                () -> apply(message)
        );
        try {
            if (!ackSubmitter.submit(ack(message, session, outcome))) {
                LOGGER.warn(
                        "DDC ACK delivery rejected for changeId={} instanceId={}",
                        message.getChangeId(),
                        session.instanceId()
                );
            }
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "DDC ACK delivery failed for changeId={} instanceId={}",
                    message.getChangeId(),
                    session.instanceId()
            );
        }
    }

    /**
     * 要求快照表示具有正版本号的唯一 YAML 资源。
     * Requires a snapshot to represent the sole YAML resource with a positive version.
     *
     * @param config 待校验配置快照; configuration snapshot to validate
     * @throws IllegalArgumentException 快照无效时抛出; thrown when the snapshot is invalid
     */
    private void requireYamlResource(DdcConfigValue config) {
        if (config == null
                || !resourceName.equals(config.getResourceName())
                || !format.equals(config.getFormat())
                || config.getVersion() == null
                || config.getVersion() <= 0) {
            throw new IllegalArgumentException(
                    "DDC scope resource must match the imported YAML resource"
            );
        }
    }

    /**
     * 校验发布消息的标识、资源、版本和资源校验和。
     * Validates publication identifiers, resource, version, and resource checksum.
     *
     * @param message 发布消息; publication message
     * @return 消息有效时为 {@code true}; {@code true} when the message is valid
     */
    private boolean isValid(DdcPublishMessage message) {
        return message != null
                && hasText(message.getChangeId())
                && resourceName.equals(message.getResourceName())
                && format.equals(message.getFormat())
                && message.getTargetVersion() != null
                && message.getTargetVersion() > 0
                && hasText(message.getResourceChecksum())
                && message.getResourceChecksum().equals(
                DdcChecksum.resource(
                        message.getResourceName(),
                        message.getFormat(),
                        message.getContent()
                )
        );
    }

    /**
     * 判断字符串包含非空白文本。
     * Indicates whether a string contains nonblank text.
     *
     * @param value 待检查文本; text to inspect
     * @return 包含文本时为 {@code true}; {@code true} when text is present
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 比较发布版本并返回成功、忽略、冲突或应用失败的 ACK 结果。
     * Compares the publication version and returns a successful, ignored, conflicting, or failed ACK outcome.
     *
     * @param message 已校验发布消息; validated publication message
     * @return ACK 结果; ACK outcome
     */
    private AckOutcome apply(DdcPublishMessage message) {
        ConfigMetadata local = metadata();
        VersionRelation relation = compare(
                local,
                message.getTargetVersion(),
                message.getResourceChecksum()
        );
        if (relation == VersionRelation.STALE) {
            return new AckOutcome(
                    DdcAckStatus.IGNORED,
                    local.version(),
                    null
            );
        }
        if (relation == VersionRelation.SAME_CONTENT) {
            return new AckOutcome(
                    DdcAckStatus.SUCCESS,
                    local.version(),
                    null
            );
        }
        if (relation == VersionRelation.CHECKSUM_CONFLICT) {
            return new AckOutcome(
                    DdcAckStatus.FAILED,
                    local.version(),
                    "DDC config checksum conflict"
            );
        }
        try {
            applyAndStore(
                    message.getContent(),
                    message.getTargetVersion(),
                    message.getResourceChecksum(),
                    message.getChangeId(),
                    local
            );
            return new AckOutcome(
                    DdcAckStatus.SUCCESS,
                    message.getTargetVersion(),
                    null
            );
        } catch (RuntimeException exception) {
            return new AckOutcome(
                    DdcAckStatus.FAILED,
                    local.version(),
                    safeErrorMessage(exception)
            );
        }
    }

    /**
     * 读取当前 YAML 资源的本地版本和校验和。
     * Reads the local version and checksum of the current YAML resource.
     *
     * @return 本地配置元数据; local configuration metadata
     */
    private ConfigMetadata metadata() {
        return new ConfigMetadata(
                repository.version(resourceName),
                repository.checksum(resourceName)
        );
    }

    /**
     * 比较目标版本和校验和与本地元数据的关系。
     * Compares the target version and checksum with local metadata.
     *
     * @param local          本地配置元数据; local configuration metadata
     * @param targetVersion  目标版本; target version
     * @param targetChecksum 目标资源校验和; target resource checksum
     * @return 目标与本地版本关系; relation between target and local versions
     */
    private VersionRelation compare(ConfigMetadata local,
                                    long targetVersion,
                                    String targetChecksum) {
        if (local.version() == null
                || targetVersion > local.version()) {
            return VersionRelation.NEWER;
        }
        if (targetVersion < local.version()) {
            return VersionRelation.STALE;
        }
        return targetChecksum.equals(local.checksum())
                ? VersionRelation.SAME_CONTENT
                : VersionRelation.CHECKSUM_CONFLICT;
    }

    /**
     * 应用 YAML 并保存对应版本和校验和，失败时恢复旧元数据。
     * Applies YAML and stores its version and checksum, restoring previous metadata on failure.
     *
     * @param content  YAML 内容; YAML content
     * @param version  目标版本; target version
     * @param checksum 资源校验和; resource checksum
     * @param changeId 发布变化标识，可为空; publication change identifier, possibly null
     * @param previous 失败回滚使用的旧元数据; previous metadata used for rollback
     */
    private void applyAndStore(String content,
                               long version,
                               String checksum,
                               @Nullable String changeId,
                               ConfigMetadata previous) {
        try {
            yamlConfigApplier.apply(content, version, changeId);
            repository.updateVersion(resourceName, version);
            repository.updateChecksum(resourceName, checksum);
        } catch (RuntimeException exception) {
            repository.restoreMetadata(
                    resourceName,
                    previous.version(),
                    previous.checksum()
            );
            throw exception;
        }
    }

    /**
     * 当本地元数据尚未建立时，从 ConfigData 当前快照初始化版本和校验和。
     * Seeds version and checksum from the current ConfigData snapshot when local metadata is absent.
     */
    private void seedConfigDataMetadata() {
        if (repository.version(resourceName) != null) {
            return;
        }
        var snapshot = yamlConfigApplier.currentSnapshot();
        repository.updateVersion(resourceName, snapshot.version());
        repository.updateChecksum(resourceName, snapshot.checksum());
    }

    /**
     * 生成不超过 ACK 长度上限的单行错误消息。
     * Produces a single-line error message within the ACK length limit.
     *
     * @param exception 配置应用异常; configuration application exception
     * @return 可安全返回管理端的错误消息; error message safe to return to the administration endpoint
     */
    private String safeErrorMessage(RuntimeException exception) {
        String prefix = "DDC config apply failed";
        String detail = exception.getMessage();
        String message = detail == null || detail.isBlank()
                ? prefix
                : prefix + ": " + detail.replaceAll("\\s+", " ").trim();
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    /**
     * 判断当前实例和租约是否位于发布目标列表中。
     * Indicates whether the current instance and lease appear in the publication target list.
     *
     * @param message 发布消息; publication message
     * @param session 当前租约会话; current lease session
     * @return 当前会话为目标时为 {@code true}; {@code true} when the current session is targeted
     */
    private boolean isTarget(DdcPublishMessage message,
                             DdcLeaseSession session) {
        DdcPublishTarget current = new DdcPublishTarget(
                session.instanceId(),
                session.leaseId()
        );
        return message.getTargets().contains(current);
    }

    /**
     * 根据发布消息、租约和应用结果构造 ACK 请求。
     * Builds an ACK request from the publication message, lease, and application outcome.
     *
     * @param message 发布消息; publication message
     * @param session 当前租约会话; current lease session
     * @param outcome 配置应用结果; configuration application outcome
     * @return 已填充的 ACK 请求; populated ACK request
     */
    private DdcAckRequest ack(DdcPublishMessage message,
                              DdcLeaseSession session,
                              AckOutcome outcome) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(message.getChangeId());
        request.setInstanceId(session.instanceId());
        request.setLeaseId(session.leaseId());
        request.setBizCode(message.getBizCode());
        request.setAppCode(message.getAppCode());
        request.setEnv(message.getEnv());
        request.setResourceName(resourceName);
        request.setTargetVersion(message.getTargetVersion());
        request.setCurrentVersion(outcome.currentVersion());
        request.setResourceChecksum(message.getResourceChecksum());
        request.setStatus(outcome.status());
        request.setErrorMessage(outcome.errorMessage());
        request.setAckTime(System.currentTimeMillis());
        return request;
    }

    /**
     * 保存发布消息处理后的 ACK 状态。
     * Holds the ACK state produced by publication-message processing.
     *
     * @param status         ACK 状态; ACK status
     * @param currentVersion 处理完成后的本地版本; local version after processing
     * @param errorMessage   失败说明，可为空; failure description, possibly null
     */
    private record AckOutcome(
            DdcAckStatus status,
            @Nullable Long currentVersion,
            @Nullable String errorMessage
    ) {
    }

    /**
     * 保存本地配置版本和校验和。
     * Holds the local configuration version and checksum.
     *
     * @param version  本地版本，可为空; local version, possibly null
     * @param checksum 本地资源校验和，可为空; local resource checksum, possibly null
     */
    private record ConfigMetadata(
            @Nullable Long version,
            @Nullable String checksum) {
    }

    /**
     * 将管理客户端适配为始终同步成功返回的 ACK 提交器。
     * Adapts an administration client to an ACK submitter that returns success after synchronous delivery.
     *
     * @param adminClient DDC 管理端客户端; DDC administration client
     * @return 同步 ACK 提交器; synchronous ACK submitter
     */
    private static AckSubmitter directAck(DdcConfigClient adminClient) {
        if (adminClient == null) {
            throw new IllegalArgumentException(
                    "adminClient must not be null"
            );
        }
        return request -> {
            adminClient.ack(request);
            return true;
        };
    }

    /**
     * 提交 ACK 并返回是否被投递机制接受的内部抽象。
     * Internal abstraction that submits an ACK and reports whether the delivery mechanism accepted it.
     */
    @FunctionalInterface
    private interface AckSubmitter {

        /**
         * 提交 ACK 请求。
         * Submits an ACK request.
         *
         * @param request ACK 请求; ACK request
         * @return 投递机制接受请求时为 {@code true}; {@code true} when the delivery mechanism accepts the request
         */
        boolean submit(DdcAckRequest request);
    }

    /**
     * 目标配置与本地配置的版本及内容关系。
     * Version and content relationship between target and local configuration.
     */
    private enum VersionRelation {
        /**
         * 目标版本更新。 Target version is newer.
         */
        NEWER,
        /**
         * 版本和内容均相同。 Version and content are identical.
         */
        SAME_CONTENT,
        /**
         * 版本相同但校验和不同。 Version matches but checksum differs.
         */
        CHECKSUM_CONFLICT,
        /**
         * 目标版本早于本地版本。 Target version is older than local.
         */
        STALE
    }
}
