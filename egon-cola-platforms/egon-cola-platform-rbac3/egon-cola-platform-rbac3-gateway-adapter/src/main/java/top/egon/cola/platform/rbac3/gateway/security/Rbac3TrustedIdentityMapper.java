package top.egon.cola.platform.rbac3.gateway.security;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 类型 `Rbac3TrustedIdentityMapper` 位于当前包内，是类型，用于承载 `Rbac3 Trusted Identity Mapper` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3TrustedIdentityMapper` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Trusted Identity Mapper`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Maps only immutable identity and version claims; authorization facts never enter headers.
 */
public final class Rbac3TrustedIdentityMapper implements GatewayIdentityMapper {

    /**
     * 字段 `MAPPER_ID` 表示 `Rbac3TrustedIdentityMapper` 中与 `MAPPER ID` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `MAPPER_ID` stores the `MAPPER ID`-related state, dependency, configuration, or result of `Rbac3TrustedIdentityMapper` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `MAPPER_ID` 时应保持 `Rbac3TrustedIdentityMapper` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `MAPPER_ID`, preserve `Rbac3TrustedIdentityMapper`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String MAPPER_ID = "rbac3-trusted-identity";

    /**
     * 方法 `mapperId` 按照 `Rbac3TrustedIdentityMapper` 的职责处理输入，完成 `mapper Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mapperId` processes its inputs according to `Rbac3TrustedIdentityMapper`'s responsibility, performs the `mapper Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mapperId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mapperId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String mapperId() {
        return MAPPER_ID;
    }

    /**
     * 方法 `supportedProtocols` 按照 `Rbac3TrustedIdentityMapper` 的职责处理输入，完成 `supported Protocols` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `supportedProtocols` processes its inputs according to `Rbac3TrustedIdentityMapper`'s responsibility, performs the `supported Protocols` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `supportedProtocols` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `supportedProtocols`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Set<GatewayProtocol> supportedProtocols() {
        return Set.of(GatewayProtocol.HTTP);
    }

    /**
     * 方法 `map` 按照 `Rbac3TrustedIdentityMapper` 的职责处理输入，完成 `map` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `map` processes its inputs according to `Rbac3TrustedIdentityMapper`'s responsibility, performs the `map` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `map` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `map`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public TrustedIdentity map(GatewayAuthContext context) {
        GatewayPrincipal principal = context.principal();
        if (!principal.authenticated() || principal.tenantId() == null) {
            throw new IllegalArgumentException("authenticated RBAC3 principal is required");
        }
        Map<String, String> attributes = principal.attributes();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-egon-gateway-tenant-id", principal.tenantId());
        headers.put("x-egon-gateway-user-id", principal.principalId());
        headers.put("x-egon-gateway-session-id", required(attributes, "rbac3.session-id"));
        headers.put("x-egon-gateway-auth-version", required(attributes, "rbac3.auth-version"));
        headers.put("x-egon-gateway-session-version", required(
                attributes, "rbac3.session-version"));
        headers.put("x-egon-gateway-policy-version", required(
                attributes, "rbac3.policy-version"));
        headers.put("x-egon-gateway-trace-id", context.traceId());
        return new TrustedIdentity(headers, Map.of());
    }

    /**
     * 方法 `required` 按照 `Rbac3TrustedIdentityMapper` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3TrustedIdentityMapper`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing verified claim " + name);
        }
        return value;
    }
}
