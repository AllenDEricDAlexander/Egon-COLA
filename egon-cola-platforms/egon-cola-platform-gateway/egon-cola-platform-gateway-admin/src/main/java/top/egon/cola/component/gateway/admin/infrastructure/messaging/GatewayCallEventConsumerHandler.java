package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.observability.GatewayCallEventIngestService;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityStore;

import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayCallEventConsumerHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责网关调用事件消费者处理器相关的职责与边界。
 * English summary: {@code GatewayCallEventConsumerHandler} is a gateway call event consumer handler handler in the current Gateway module; it owns the gateway call event consumer handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCallEventConsumerHandler {

    /**
     * 中文说明：保存 codec 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallEventCodec}，由 {@code GatewayCallEventConsumerHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by codec; its type is {@code GatewayCallEventCodec}, and {@code GatewayCallEventConsumerHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventConsumerHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventConsumerHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallEventCodec codec;

    /**
     * 中文说明：保存 ingest服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallEventIngestService}，由 {@code GatewayCallEventConsumerHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by ingest service; its type is {@code GatewayCallEventIngestService}, and {@code GatewayCallEventConsumerHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventConsumerHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventConsumerHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallEventIngestService ingestService;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayCallEventConsumerHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayCallEventConsumerHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventConsumerHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventConsumerHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayCallEventConsumerHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCallEventConsumerHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param codec 参数 codec；parameter codec。
     * @param ingestService 参数 ingest服务；parameter ingest service。
     * @param clock 参数 clock；parameter clock。
     */
    public GatewayCallEventConsumerHandler(
            GatewayCallEventCodec codec,
            GatewayCallEventIngestService ingestService,
            Clock clock) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.ingestService = Objects.requireNonNull(
                ingestService,
                "ingestService"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Returns only after the projection or poison record transaction commits.
     * 补充说明 / Supplementary summary: 执行 handle 操作；该方法是 {@code GatewayCallEventConsumerHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the handle operation; this method is the invocation entry point on {@code GatewayCallEventConsumerHandler} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventConsumerHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public Result handle(ConsumerRecord<String, byte[]> record) {
        try {
            boolean inserted = ingestService.ingest(codec.decode(
                    record.value()
            ));
            return inserted ? Result.PROJECTED : Result.DUPLICATE;
        } catch (IllegalArgumentException poison) {
            recordFailure(
                    record,
                    "GATEWAY_CALL_EVENT_INVALID",
                    poison
            );
            return Result.POISON_RECORDED;
        }
    }

    /**
     * 中文说明：执行 deadLetter 操作；该方法是 {@code GatewayCallEventConsumerHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dead letter operation; this method is the invocation entry point on {@code GatewayCallEventConsumerHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventConsumerHandler.deadLetter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param record 参数 record；parameter record。
     * @param failure 参数 failure；parameter failure。
     */
    public void deadLetter(
            ConsumerRecord<String, byte[]> record,
            RuntimeException failure) {
        recordFailure(
                record,
                "GATEWAY_CALL_EVENT_PROCESSING_FAILED",
                failure
        );
    }

    /**
     * 中文说明：执行 recordFailure 操作；该方法是 {@code GatewayCallEventConsumerHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record failure operation; this method is the invocation entry point on {@code GatewayCallEventConsumerHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventConsumerHandler.recordFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param record 参数 record；parameter record。
     * @param failureCode 参数 failureCode；parameter failure code。
     * @param failure 参数 failure；parameter failure。
     */
    private void recordFailure(
            ConsumerRecord<String, byte[]> record,
            String failureCode,
            RuntimeException failure) {
        byte[] payload = record.value() == null
                ? new byte[0]
                : record.value();
        ingestService.poison(
                new GatewayObservabilityStore.ConsumeFailure(
                        UuidV7.simpleString(),
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        header(record, "event-id"),
                        failureCode,
                        bounded(failure.getMessage()),
                        sha256(payload),
                        payload.length,
                        clock.instant()
                )
        );
    }

    /**
     * 中文说明：执行 header 操作；该方法是 {@code GatewayCallEventConsumerHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the header operation; this method is the invocation entry point on {@code GatewayCallEventConsumerHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventConsumerHandler.header(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param record 参数 record；parameter record。
     * @param name 参数 name；parameter name。
     * @return 返回 header 的处理结果；returns the result of the operation.
     */
    private String header(
            ConsumerRecord<String, byte[]> record,
            String name) {
        org.apache.kafka.common.header.Header header =
                record.headers().lastHeader(name);
        return header == null
                ? null
                : new String(
                        header.value(),
                        java.nio.charset.StandardCharsets.UTF_8
                );
    }

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code GatewayCallEventConsumerHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code GatewayCallEventConsumerHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventConsumerHandler.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param payload 参数 payload；parameter payload。
     * @return 返回 sha256 的处理结果；returns the result of the operation.
     */
    private String sha256(byte[] payload) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(payload)
            );
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * 中文说明：执行 bounded 操作；该方法是 {@code GatewayCallEventConsumerHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bounded operation; this method is the invocation entry point on {@code GatewayCallEventConsumerHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventConsumerHandler.bounded(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 bounded 的处理结果；returns the result of the operation.
     */
    private String bounded(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1024 ? value : value.substring(0, 1024);
    }

    /**
     * 中文说明：{@code Result} 是枚举类型，位于当前 Gateway 模块的相关包中，负责Result相关的职责与边界。
     * English summary: {@code Result} is an enumeration in the current Gateway module; it owns the result-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public enum Result {
        /**
         * 中文说明：表示 PROJECTED 这一固定值；它属于 {@code GatewayCallEventConsumerHandler.Result} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value projected; it is a state, type, or protocol value of {@code GatewayCallEventConsumerHandler.Result} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCallEventConsumerHandler.Result} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventConsumerHandler.Result}; do not couple callers to its representation when the owning type exposes an API.
         */
        PROJECTED,
        /**
         * 中文说明：表示 DUPLICATE 这一固定值；它属于 {@code GatewayCallEventConsumerHandler.Result} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value duplicate; it is a state, type, or protocol value of {@code GatewayCallEventConsumerHandler.Result} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCallEventConsumerHandler.Result} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventConsumerHandler.Result}; do not couple callers to its representation when the owning type exposes an API.
         */
        DUPLICATE,
        /**
         * 中文说明：表示 POISONRECORDED 这一固定值；它属于 {@code GatewayCallEventConsumerHandler.Result} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value poison recorded; it is a state, type, or protocol value of {@code GatewayCallEventConsumerHandler.Result} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCallEventConsumerHandler.Result} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventConsumerHandler.Result}; do not couple callers to its representation when the owning type exposes an API.
         */
        POISON_RECORDED
    }
}
