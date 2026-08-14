package top.egon.cola.component.gateway.engine.security;

import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayRouteSecurityType;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code GatewaySecurityResult} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关安全Result相关的职责与边界。
 * English summary: {@code GatewaySecurityResult} is an immutable data carrier in the current Gateway module; it owns the gateway security result-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param context 参数 context；parameter context。
 * @param trustedIdentity 参数 trusted身份；parameter trusted identity。
 * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
 * @param forwardingCredential 参数 forwarding凭证；parameter forwarding credential。
 */
public record GatewaySecurityResult(
        /**
         * 中文说明：保存 context 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuthContext}，由 {@code GatewaySecurityResult} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by context; its type is {@code GatewayAuthContext}, and {@code GatewaySecurityResult} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewaySecurityResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityResult}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayAuthContext context,
        /**
         * 中文说明：保存 trusted身份 对应的状态、依赖或配置值；字段类型为 {@code TrustedIdentity}，由 {@code GatewaySecurityResult} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trusted identity; its type is {@code TrustedIdentity}, and {@code GatewaySecurityResult} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewaySecurityResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityResult}; do not couple callers to its representation when the owning type exposes an API.
         */
        TrustedIdentity trustedIdentity,
        /**
         * 中文说明：保存 fieldsToRemove 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code GatewaySecurityResult} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by fields to remove; its type is {@code Set<String>}, and {@code GatewaySecurityResult} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewaySecurityResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityResult}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> fieldsToRemove,
        /**
         * 中文说明：保存 forwarding凭证 对应的状态、依赖或配置值；字段类型为 {@code GatewayCredential}，由 {@code GatewaySecurityResult} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by forwarding credential; its type is {@code GatewayCredential}, and {@code GatewaySecurityResult} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewaySecurityResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityResult}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayCredential forwardingCredential,
        Map<String, List<String>> responseHeaders,
        GatewayRouteSecurityType routeSecurityType
) {

    /**
     * 中文说明：创建 {@code GatewaySecurityResult} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewaySecurityResult} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param context 参数 context；parameter context。
     * @param trustedIdentity 参数 trusted身份；parameter trusted identity。
     * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
     * @param forwardingCredential 参数 forwarding凭证；parameter forwarding credential。
     */
    public GatewaySecurityResult {
        context = Objects.requireNonNull(context, "context");
        trustedIdentity = Objects.requireNonNull(
                trustedIdentity,
                "trustedIdentity"
        );
        fieldsToRemove = Set.copyOf(Objects.requireNonNull(
                fieldsToRemove,
                "fieldsToRemove"
        ));
        responseHeaders = Map.copyOf(Objects.requireNonNull(
                responseHeaders,
                "responseHeaders"
        ));
        routeSecurityType = Objects.requireNonNull(
                routeSecurityType,
                "routeSecurityType"
        );
    }

    /**
     * 中文说明：创建 {@code GatewaySecurityResult} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewaySecurityResult} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param context 参数 context；parameter context。
     * @param trustedIdentity 参数 trusted身份；parameter trusted identity。
     * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
     */
    public GatewaySecurityResult(
            GatewayAuthContext context,
            TrustedIdentity trustedIdentity,
            Set<String> fieldsToRemove
    ) {
        this(context, trustedIdentity, fieldsToRemove, null);
    }

    public GatewaySecurityResult(
            GatewayAuthContext context,
            TrustedIdentity trustedIdentity,
            Set<String> fieldsToRemove,
            GatewayCredential forwardingCredential) {
        this(context, trustedIdentity, fieldsToRemove, forwardingCredential,
                Map.of(), GatewayRouteSecurityType.BUSINESS_PROTECTED);
    }

    public GatewaySecurityResult(
            GatewayAuthContext context,
            TrustedIdentity trustedIdentity,
            Set<String> fieldsToRemove,
            GatewayCredential forwardingCredential,
            Map<String, List<String>> responseHeaders) {
        this(context, trustedIdentity, fieldsToRemove, forwardingCredential,
                responseHeaders, GatewayRouteSecurityType.BUSINESS_PROTECTED);
    }

    /**
     * 中文说明：执行 toString 操作；该方法是 {@code GatewaySecurityResult} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the to string operation; this method is the invocation entry point on {@code GatewaySecurityResult} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityResult.toString(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 toString 的处理结果；returns the result of the operation.
     */
    @Override
    public String toString() {
        return "GatewaySecurityResult[context=" + context
                + ", trustedIdentity=" + trustedIdentity
                + ", fieldsToRemove=" + fieldsToRemove
                + ", forwardingCredential="
                + (forwardingCredential == null ? "NONE" : "REDACTED")
                + ']';
    }
}
