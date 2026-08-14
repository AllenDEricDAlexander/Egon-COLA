package top.egon.cola.component.gateway.admin.release.controller.scheduled;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.gateway.admin.rule.service.GatewayDdcYamlDocument;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：{@code GatewayRuleChunkGarbageCollector} 是类型，位于当前 Gateway 模块的相关包中，负责网关规则ChunkGarbageCollector相关的职责与边界。
 * English summary: {@code GatewayRuleChunkGarbageCollector} is a type in the current Gateway module; it owns the gateway rule chunk garbage collector-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Component
public class GatewayRuleChunkGarbageCollector {

    /**
     * 中文说明：表示 LOGGER 这一固定值；它属于 {@code GatewayRuleChunkGarbageCollector} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value logger; it is a state, type, or protocol value of {@code GatewayRuleChunkGarbageCollector} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            GatewayRuleChunkGarbageCollector.class
    );

    /**
     * 中文说明：保存 journal 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleasePublicationRepository}，由 {@code GatewayRuleChunkGarbageCollector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by journal; its type is {@code GatewayReleasePublicationRepository}, and {@code GatewayRuleChunkGarbageCollector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleasePublicationRepository journal;

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code DdcManagementClient}，由 {@code GatewayRuleChunkGarbageCollector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code DdcManagementClient}, and {@code GatewayRuleChunkGarbageCollector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcManagementClient client;

    /**
     * 中文说明：保存 properties 对应的状态、依赖或配置值；字段类型为 {@code GatewayAdminProperties}，由 {@code GatewayRuleChunkGarbageCollector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by properties; its type is {@code GatewayAdminProperties}, and {@code GatewayRuleChunkGarbageCollector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAdminProperties properties;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayRuleChunkGarbageCollector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayRuleChunkGarbageCollector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 publish超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayRuleChunkGarbageCollector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by publish timeout; its type is {@code Duration}, and {@code GatewayRuleChunkGarbageCollector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration publishTimeout;

    /**
     * 中文说明：保存 yamlDocument 对应的状态、依赖或配置值；字段类型为 {@code GatewayDdcYamlDocument}，由 {@code GatewayRuleChunkGarbageCollector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by yaml document; its type is {@code GatewayDdcYamlDocument}, and {@code GatewayRuleChunkGarbageCollector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDdcYamlDocument yamlDocument;

    /**
     * 中文说明：保存 deleted 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayRuleChunkGarbageCollector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by deleted; its type is {@code AtomicLong}, and {@code GatewayRuleChunkGarbageCollector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong deleted = new AtomicLong();

    /**
     * 中文说明：保存 failed 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayRuleChunkGarbageCollector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by failed; its type is {@code AtomicLong}, and {@code GatewayRuleChunkGarbageCollector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkGarbageCollector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkGarbageCollector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong failed = new AtomicLong();

    /**
     * 中文说明：创建 {@code GatewayRuleChunkGarbageCollector} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRuleChunkGarbageCollector} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param journal 参数 journal；parameter journal。
     * @param client 参数 客户端；parameter client。
     * @param properties 参数 properties；parameter properties。
     * @param publishTimeout 参数 publish超时；parameter publish timeout。
     */
    @Autowired
    public GatewayRuleChunkGarbageCollector(
            GatewayReleasePublicationRepository journal,
            ObjectProvider<DdcManagementClient> client,
            GatewayAdminProperties properties,
            @Value("${gateway.admin.ddc.publish-timeout:PT30S}")
            Duration publishTimeout) {
        this(
                journal,
                client.getIfAvailable(),
                properties,
                Clock.systemUTC(),
                publishTimeout
        );
    }

    /**
     * 中文说明：创建 {@code GatewayRuleChunkGarbageCollector} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRuleChunkGarbageCollector} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param journal 参数 journal；parameter journal。
     * @param client 参数 客户端；parameter client。
     * @param properties 参数 properties；parameter properties。
     * @param clock 参数 clock；parameter clock。
     * @param publishTimeout 参数 publish超时；parameter publish timeout。
     */
    GatewayRuleChunkGarbageCollector(
            GatewayReleasePublicationRepository journal,
            DdcManagementClient client,
            GatewayAdminProperties properties,
            Clock clock,
            Duration publishTimeout) {
        this.journal = Objects.requireNonNull(journal, "journal");
        this.client = client;
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.publishTimeout = positive(
                publishTimeout,
                "Gateway DDC publish timeout"
        );
        this.yamlDocument = new GatewayDdcYamlDocument();
        retention();
    }

    /**
     * 中文说明：执行 collect 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the collect operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.collect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.rule-chunk.cleanup-delay:PT1H}"
    )
    public void collect() {
        collectOnce();
    }

    /**
     * 中文说明：执行 collectOnce 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the collect once operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.collectOnce(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public void collectOnce() {
        if (client == null) {
            return;
        }
        journal.findChunkCleanupCandidates(
                clock.instant().minus(retention())
        ).forEach(this::delete);
    }

    /**
     * 中文说明：执行 deletedCount 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the deleted count operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.deletedCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 deletedCount 的处理结果；returns the result of the operation.
     */
    public long deletedCount() {
        return deleted.get();
    }

    /**
     * 中文说明：执行 failedCount 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the failed count operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.failedCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 failedCount 的处理结果；returns the result of the operation.
     */
    public long failedCount() {
        return failed.get();
    }

    /**
     * 中文说明：执行 delete 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.delete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param candidate 参数 candidate；parameter candidate。
     */
    private void delete(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO candidate) {
        String changeId = UuidV7.string();
        try {
            DdcManagementConfig config = current(candidate).orElse(null);
            if (config == null || config.deleted()) {
                markCleaned(candidate);
                return;
            }
            validate(config);
            top.egon.cola.component.gateway.admin.rule.service.GatewayYamlRemoval removal =
                    yamlDocument.removeLeaf(
                            config.content(),
                            candidate.configKey()
                    );
            if (!removal.removed()) {
                markCleaned(candidate);
                return;
            }
            DdcManagementPublishResult result = client.publish(
                    new DdcManagementPublishRequest(
                            properties.getDdc().getTargetBizCode(),
                            candidate.env(),
                            properties.getDdc().getTargetAppCode(),
                            config.resourceName(),
                            removal.content(),
                            config.format(),
                            config.version(),
                            changeId,
                            publishTimeout.toMillis(),
                            "gateway_rule_chunk_gc"
                    )
            );
            if (result.status() != DdcManagementPublishStatus.SUCCESS) {
                recordFailure(candidate, new IllegalStateException(
                        "Gateway rule chunk cleanup publish did not succeed"
                ));
                return;
            }
        } catch (RuntimeException failure) {
            Optional<DdcManagementPublishStatus> recovered =
                    publishStatus(changeId);
            if (recovered.isPresent()
                    && recovered.get()
                    != DdcManagementPublishStatus.SUCCESS) {
                recordFailure(candidate, failure);
                return;
            }
            if (recovered.isEmpty() && !alreadyDeleted(candidate)) {
                recordFailure(candidate, failure);
                return;
            }
        }
        markCleaned(candidate);
    }

    /**
     * 中文说明：执行 markCleaned 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mark cleaned operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.markCleaned(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param candidate 参数 candidate；parameter candidate。
     */
    private void markCleaned(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO candidate) {
        try {
            journal.markChunkCleaned(
                    candidate.changeId(),
                    clock.instant()
            );
            deleted.incrementAndGet();
        } catch (RuntimeException failure) {
            recordFailure(candidate, failure);
        }
    }

    /**
     * 中文说明：执行 alreadyDeleted 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the already deleted operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.alreadyDeleted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param candidate 参数 candidate；parameter candidate。
     * @return 返回 alreadyDeleted 的处理结果；returns the result of the operation.
     */
    private boolean alreadyDeleted(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO candidate) {
        try {
            return current(candidate)
                    .map(config -> config.deleted()
                            || yamlDocument.leafValue(
                                    config.content(),
                                    candidate.configKey()
                            ).isEmpty())
                    .orElse(true);
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    /**
     * 中文说明：执行 current 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.current(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param candidate 参数 candidate；parameter candidate。
     * @return 返回 current 的处理结果；returns the result of the operation.
     */
    private Optional<DdcManagementConfig> current(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO candidate) {
        return client.findConfig(new DdcManagementConfigQuery(
                properties.getDdc().getTargetBizCode(),
                candidate.env(),
                properties.getDdc().getTargetAppCode()
        ));
    }

    /**
     * 中文说明：执行 publishStatus 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish status operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.publishStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param changeId 参数 changeId；parameter change id。
     * @return 返回 publishStatus 的处理结果；returns the result of the operation.
     */
    private Optional<DdcManagementPublishStatus> publishStatus(
            String changeId) {
        try {
            DdcManagementPublishTask task = client.getPublishTask(changeId);
            return task == null
                    ? Optional.empty()
                    : Optional.of(task.status());
        } catch (RuntimeException unavailable) {
            return Optional.empty();
        }
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param config 参数 config；parameter config。
     */
    private void validate(DdcManagementConfig config) {
        if (config.version() == null || config.version() < 0
                || !config.enabled()
                || !GatewayDdcYamlDocument.RESOURCE_NAME.equals(
                config.resourceName())
                || !GatewayDdcYamlDocument.FORMAT.equals(config.format())) {
            throw new IllegalStateException(
                    "Gateway rule cleanup requires an enabled "
                            + "application.yml/YAML document"
            );
        }
    }

    /**
     * 中文说明：执行 recordFailure 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record failure operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.recordFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param candidate 参数 candidate；parameter candidate。
     * @param failure 参数 failure；parameter failure。
     */
    private void recordFailure(
            top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO candidate,
            RuntimeException failure) {
        failed.incrementAndGet();
        LOGGER.warn(
                "Gateway rule chunk cleanup failed releaseId={} "
                        + "configKey={} targetVersion={} cause={}",
                candidate.releaseId(),
                candidate.configKey(),
                candidate.targetVersion(),
                failure.getClass().getSimpleName()
        );
    }

    /**
     * 中文说明：执行 retention 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retention operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.retention(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 retention 的处理结果；returns the result of the operation.
     */
    private Duration retention() {
        return positive(
                properties.getRuleChunk().getRetention(),
                "gateway rule chunk retention"
        );
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code GatewayRuleChunkGarbageCollector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code GatewayRuleChunkGarbageCollector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkGarbageCollector.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param name 参数 name；parameter name。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
