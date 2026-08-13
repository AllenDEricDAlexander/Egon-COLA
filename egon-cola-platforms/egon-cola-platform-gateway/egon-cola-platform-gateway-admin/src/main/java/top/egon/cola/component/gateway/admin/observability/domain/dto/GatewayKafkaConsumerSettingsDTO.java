package top.egon.cola.component.gateway.admin.observability.domain.dto;


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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.FencedInstanceIdException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRecordKey;
import top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRebalanceListener;

/**
 * 中文说明：{@code GatewayKafkaConsumerSettingsDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Settings相关的职责与边界。
 * English summary: {@code GatewayKafkaConsumerSettingsDTO} is an immutable data carrier in the current Gateway module; it owns the settings-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param bootstrapServers 参数 bootstrapServers；parameter bootstrap servers。
 * @param topic 参数 topic；parameter topic。
 * @param groupId 参数 groupId；parameter group id。
 * @param pollTimeout 参数 poll超时；parameter poll timeout。
 * @param commitTimeout 参数 commit超时；parameter commit timeout。
 * @param closeTimeout 参数 close超时；parameter close timeout。
 * @param retryBackoff 参数 重试Backoff；parameter retry backoff。
 * @param maxRecordAttempts 参数 maxRecordAttempts；parameter max record attempts。
 * @param additionalProperties 参数 additionalProperties；parameter additional properties。
 */
public record GatewayKafkaConsumerSettingsDTO(
        /**
         * 中文说明：保存 bootstrapServers 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by bootstrap servers; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bootstrapServers,
        /**
         * 中文说明：保存 topic 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by topic; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String topic,
        /**
         * 中文说明：保存 groupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String groupId,
        /**
         * 中文说明：保存 poll超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by poll timeout; its type is {@code Duration}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration pollTimeout,
        /**
         * 中文说明：保存 commit超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by commit timeout; its type is {@code Duration}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration commitTimeout,
        /**
         * 中文说明：保存 close超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by close timeout; its type is {@code Duration}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration closeTimeout,
        /**
         * 中文说明：保存 重试Backoff 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by retry backoff; its type is {@code Duration}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration retryBackoff,
        /**
         * 中文说明：保存 maxRecordAttempts 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max record attempts; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maxRecordAttempts,
        /**
         * 中文说明：保存 additionalProperties 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by additional properties; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> additionalProperties
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param bootstrapServers 参数 bootstrapServers；parameter bootstrap servers。
     * @param topic 参数 topic；parameter topic。
     * @param groupId 参数 groupId；parameter group id。
     * @param pollTimeout 参数 poll超时；parameter poll timeout。
     * @param commitTimeout 参数 commit超时；parameter commit timeout。
     * @param closeTimeout 参数 close超时；parameter close timeout。
     * @param additionalProperties 参数 additionalProperties；parameter additional properties。
     */
    public GatewayKafkaConsumerSettingsDTO(
            String bootstrapServers,
            String topic,
            String groupId,
            Duration pollTimeout,
            Duration commitTimeout,
            Duration closeTimeout,
            Map<String, Object> additionalProperties) {
        this(
                bootstrapServers,
                topic,
                groupId,
                pollTimeout,
                commitTimeout,
                closeTimeout,
                Duration.ofMillis(250),
                5,
                additionalProperties
        );
    }

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param bootstrapServers 参数 bootstrapServers；parameter bootstrap servers。
     * @param topic 参数 topic；parameter topic。
     * @param groupId 参数 groupId；parameter group id。
     * @param pollTimeout 参数 poll超时；parameter poll timeout。
     * @param commitTimeout 参数 commit超时；parameter commit timeout。
     * @param closeTimeout 参数 close超时；parameter close timeout。
     * @param retryBackoff 参数 重试Backoff；parameter retry backoff。
     * @param maxRecordAttempts 参数 maxRecordAttempts；parameter max record attempts。
     * @param additionalProperties 参数 additionalProperties；parameter additional properties。
     */
    public GatewayKafkaConsumerSettingsDTO {
        pollTimeout = pollTimeout == null
                ? Duration.ofMillis(500)
                : pollTimeout;
        commitTimeout = commitTimeout == null
                ? Duration.ofSeconds(5)
                : commitTimeout;
        closeTimeout = closeTimeout == null
                ? Duration.ofSeconds(5)
                : closeTimeout;
        retryBackoff = retryBackoff == null
                ? Duration.ofMillis(250)
                : retryBackoff;
        if (retryBackoff.isNegative()
                || retryBackoff.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalArgumentException(
                    "retryBackoff must be between PT0S and PT30S"
            );
        }
        if (maxRecordAttempts < 1 || maxRecordAttempts > 100) {
            throw new IllegalArgumentException(
                    "maxRecordAttempts must be between 1 and 100"
            );
        }
        additionalProperties = additionalProperties == null
                ? Map.of()
                : Map.copyOf(additionalProperties);
    }
}
