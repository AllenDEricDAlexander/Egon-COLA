package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code GatewayHttpSecurityProcessor} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关Http安全Processor相关的职责与边界。
 * English summary: {@code GatewayHttpSecurityProcessor} is an interface contract in the current Gateway module; it owns the gateway http security processor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayHttpSecurityProcessor {

    /**
     * 中文说明：执行 authorize 操作；该方法是 {@code GatewayHttpSecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize operation; this method is the invocation entry point on {@code GatewayHttpSecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpSecurityProcessor.authorize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param request 参数 请求；parameter request。
     * @param normalized 参数 normalized；parameter normalized。
     * @param route 参数 路由；parameter route。
     * @param traceId 参数 traceId；parameter trace id。
     * @return 返回 authorize 的处理结果；returns the result of the operation.
     */
    Mono<Outcome> authorize(
            AccessZone accessZone,
            GatewayInboundHttpRequest request,
            NormalizedHttpRequest normalized,
            HttpRouteMatch route,
            String traceId);

    /**
     * 中文说明：{@code Outcome} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Outcome相关的职责与边界。
     * English summary: {@code Outcome} is an immutable data carrier in the current Gateway module; it owns the outcome-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param trustedIdentity 参数 trusted身份；parameter trusted identity。
     * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
     * @param forwardingCredential 参数 forwarding凭证；parameter forwarding credential。
     */
    record Outcome(
            /**
             * 中文说明：保存 trusted身份 对应的状态、依赖或配置值；字段类型为 {@code TrustedIdentity}，由 {@code GatewayHttpSecurityProcessor.Outcome} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trusted identity; its type is {@code TrustedIdentity}, and {@code GatewayHttpSecurityProcessor.Outcome} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayHttpSecurityProcessor.Outcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpSecurityProcessor.Outcome}; do not couple callers to its representation when the owning type exposes an API.
             */
            TrustedIdentity trustedIdentity,
            /**
             * 中文说明：保存 fieldsToRemove 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code GatewayHttpSecurityProcessor.Outcome} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by fields to remove; its type is {@code Set<String>}, and {@code GatewayHttpSecurityProcessor.Outcome} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayHttpSecurityProcessor.Outcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpSecurityProcessor.Outcome}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> fieldsToRemove,
            /**
             * 中文说明：保存 forwarding凭证 对应的状态、依赖或配置值；字段类型为 {@code GatewayCredential}，由 {@code GatewayHttpSecurityProcessor.Outcome} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by forwarding credential; its type is {@code GatewayCredential}, and {@code GatewayHttpSecurityProcessor.Outcome} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayHttpSecurityProcessor.Outcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpSecurityProcessor.Outcome}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayCredential forwardingCredential
    ) {

        /**
         * 中文说明：创建 {@code GatewayHttpSecurityProcessor.Outcome} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayHttpSecurityProcessor.Outcome} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param trustedIdentity 参数 trusted身份；parameter trusted identity。
         * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
         * @param forwardingCredential 参数 forwarding凭证；parameter forwarding credential。
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
         * 中文说明：创建 {@code GatewayHttpSecurityProcessor.Outcome} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayHttpSecurityProcessor.Outcome} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param trustedIdentity 参数 trusted身份；parameter trusted identity。
         * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
         */
        public Outcome(
                TrustedIdentity trustedIdentity,
                Set<String> fieldsToRemove
        ) {
            this(trustedIdentity, fieldsToRemove, null);
        }

        /**
         * 中文说明：执行 anonymous 操作；该方法是 {@code GatewayHttpSecurityProcessor.Outcome} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the anonymous operation; this method is the invocation entry point on {@code GatewayHttpSecurityProcessor.Outcome} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpSecurityProcessor.Outcome.anonymous(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 anonymous 的处理结果；returns the result of the operation.
         */
        public static Outcome anonymous() {
            return new Outcome(TrustedIdentity.empty(), Set.of(), null);
        }
    }
}
