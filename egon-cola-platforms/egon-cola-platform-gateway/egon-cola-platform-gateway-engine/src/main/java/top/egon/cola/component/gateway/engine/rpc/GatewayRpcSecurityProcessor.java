package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.Deadline;
import io.grpc.Metadata;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code GatewayRpcSecurityProcessor} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关Rpc安全Processor相关的职责与边界。
 * English summary: {@code GatewayRpcSecurityProcessor} is an interface contract in the current Gateway module; it owns the gateway rpc security processor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayRpcSecurityProcessor {

    /**
     * 中文说明：执行 authorize 操作；该方法是 {@code GatewayRpcSecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize operation; this method is the invocation entry point on {@code GatewayRpcSecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRpcSecurityProcessor.authorize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @param inboundMetadata 参数 inbound元数据；parameter inbound metadata。
     * @param traceId 参数 traceId；parameter trace id。
     * @param deadline 参数 deadline；parameter deadline。
     * @return 返回 authorize 的处理结果；returns the result of the operation.
     */
    Mono<Outcome> authorize(
            RuntimeRpcRoute route,
            Metadata inboundMetadata,
            String traceId,
            Deadline deadline);

    /**
     * 中文说明：{@code Outcome} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Outcome相关的职责与边界。
     * English summary: {@code Outcome} is an immutable data carrier in the current Gateway module; it owns the outcome-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param trustedIdentity 参数 trusted身份；parameter trusted identity。
     * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
     * @param forwardingCredential 经过安全链验证、允许向下游中继的凭据；verified credential
     *                             approved for downstream relay
     */
    record Outcome(
            /**
             * 中文说明：保存 trusted身份 对应的状态、依赖或配置值；字段类型为 {@code TrustedIdentity}，由 {@code GatewayRpcSecurityProcessor.Outcome} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trusted identity; its type is {@code TrustedIdentity}, and {@code GatewayRpcSecurityProcessor.Outcome} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayRpcSecurityProcessor.Outcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRpcSecurityProcessor.Outcome}; do not couple callers to its representation when the owning type exposes an API.
             */
            TrustedIdentity trustedIdentity,
            /**
             * 中文说明：保存 fieldsToRemove 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code GatewayRpcSecurityProcessor.Outcome} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by fields to remove; its type is {@code Set<String>}, and {@code GatewayRpcSecurityProcessor.Outcome} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayRpcSecurityProcessor.Outcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRpcSecurityProcessor.Outcome}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> fieldsToRemove,
            /**
             * 中文说明：保存经过认证且允许向下游中继的敏感凭据；该值只能来自安全链结果，不能由入站 Metadata 重建。
             * English summary: Holds the authenticated credential approved for downstream relay;
             * it may only come from the security-chain result and must not be reconstructed from
             * inbound metadata.
             */
            GatewayCredential forwardingCredential
    ) {

        /**
         * 中文说明：创建 {@code GatewayRpcSecurityProcessor.Outcome} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayRpcSecurityProcessor.Outcome} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param trustedIdentity 参数 trusted身份；parameter trusted identity。
         * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
         * @param forwardingCredential 已验证的下游中继凭据；verified downstream relay
         *                             credential
         */
        public Outcome {
            trustedIdentity = Objects.requireNonNull(
                    trustedIdentity,
                    "trustedIdentity"
            );
            fieldsToRemove = Set.copyOf(Objects.requireNonNull(
                    fieldsToRemove,
                    "fieldsToRemove"
            ));
        }

        /**
         * 中文说明：执行 anonymous 操作；该方法是 {@code GatewayRpcSecurityProcessor.Outcome} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the anonymous operation; this method is the invocation entry point on {@code GatewayRpcSecurityProcessor.Outcome} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayRpcSecurityProcessor.Outcome.anonymous(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 anonymous 的处理结果；returns the result of the operation.
         */
        public static Outcome anonymous() {
            return new Outcome(TrustedIdentity.empty(), Set.of(), null);
        }
    }
}
