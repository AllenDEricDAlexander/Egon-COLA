package top.egon.cola.component.gateway.admin.observability.controller.message;


import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayKafkaRebalanceListener} 是监听器，位于当前 Gateway 模块的相关包中，负责Rebalance监听器相关的职责与边界。
 * English summary: {@code GatewayKafkaRebalanceListener} is a rebalance listener listener in the current Gateway module; it owns the rebalance listener-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayKafkaRebalanceListener
        implements ConsumerRebalanceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GatewayKafkaRebalanceListener.class
    );

    private final Map<GatewayKafkaRecordKey, Integer> attempts;

    public GatewayKafkaRebalanceListener(
            Map<GatewayKafkaRecordKey, Integer> attempts) {
        this.attempts = Objects.requireNonNull(attempts, "attempts");
    }

    /**
     * 中文说明：执行 onPartitionsRevoked 操作；该方法是 {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRebalanceListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on partitions revoked operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRebalanceListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRebalanceListener.onPartitionsRevoked(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param partitions 参数 partitions；parameter partitions。
     */
    @Override
    public void onPartitionsRevoked(
            java.util.Collection<TopicPartition> partitions) {
        attempts.clear();
    }

    /**
     * 中文说明：执行 onPartitionsAssigned 操作；该方法是 {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRebalanceListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on partitions assigned operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRebalanceListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.observability.controller.message.GatewayKafkaRebalanceListener.onPartitionsAssigned(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param partitions 参数 partitions；parameter partitions。
     */
    @Override
    public void onPartitionsAssigned(
            java.util.Collection<TopicPartition> partitions) {
        LOGGER.info(
                "Gateway Kafka consumer assigned {} partitions",
                partitions.size()
        );
    }
}
