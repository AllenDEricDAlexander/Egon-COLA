package top.egon.cola.component.gateway.engine.common.security.service;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayCredentialRecoveryProvider;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 中文说明：{@code GatewaySecurityCapabilityRegistry} 是类型，位于当前 Gateway 模块的相关包中，负责网关安全Capability注册表相关的职责与边界。
 * English summary: {@code GatewaySecurityCapabilityRegistry} is a type in the current Gateway module; it owns the gateway security capability registry-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewaySecurityCapabilityRegistry {

    /**
     * 中文说明：保存 extractors 对应的状态、依赖或配置值；字段类型为 {@code Map<String, GatewayCredentialExtractor>}，由 {@code GatewaySecurityCapabilityRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by extractors; its type is {@code Map<String, GatewayCredentialExtractor>}, and {@code GatewaySecurityCapabilityRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityCapabilityRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityCapabilityRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, GatewayCredentialExtractor> extractors;

    /**
     * 中文说明：保存 authentications 对应的状态、依赖或配置值；字段类型为 {@code Map<String, GatewayAuthenticationProvider>}，由 {@code GatewaySecurityCapabilityRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by authentications; its type is {@code Map<String, GatewayAuthenticationProvider>}, and {@code GatewaySecurityCapabilityRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityCapabilityRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityCapabilityRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, GatewayAuthenticationProvider> authentications;

    /**
     * 中文说明：保存 authorizations 对应的状态、依赖或配置值；字段类型为 {@code Map<String, GatewayAuthorizationProvider>}，由 {@code GatewaySecurityCapabilityRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by authorizations; its type is {@code Map<String, GatewayAuthorizationProvider>}, and {@code GatewaySecurityCapabilityRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityCapabilityRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityCapabilityRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, GatewayAuthorizationProvider> authorizations;

    /**
     * 中文说明：保存 身份Mappers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, GatewayIdentityMapper>}，由 {@code GatewaySecurityCapabilityRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by identity mappers; its type is {@code Map<String, GatewayIdentityMapper>}, and {@code GatewaySecurityCapabilityRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityCapabilityRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityCapabilityRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, GatewayIdentityMapper> identityMappers;

    private final Map<String, GatewayCredentialRecoveryProvider> recoveries;

    /**
     * 中文说明：创建 {@code GatewaySecurityCapabilityRegistry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewaySecurityCapabilityRegistry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param extractors 参数 extractors；parameter extractors。
     * @param authentications 参数 authentications；parameter authentications。
     * @param authorizations 参数 authorizations；parameter authorizations。
     * @param identityMappers 参数 身份Mappers；parameter identity mappers。
     */
    public GatewaySecurityCapabilityRegistry(
            Collection<GatewayCredentialExtractor> extractors,
            Collection<GatewayAuthenticationProvider> authentications,
            Collection<GatewayAuthorizationProvider> authorizations,
            Collection<GatewayIdentityMapper> identityMappers,
            Collection<GatewayCredentialRecoveryProvider> recoveries) {
        this.extractors = index(
                extractors,
                GatewayCredentialExtractor::extractorId,
                "credential extractor"
        );
        this.authentications = index(
                authentications,
                GatewayAuthenticationProvider::providerId,
                "authentication provider"
        );
        this.authorizations = index(
                authorizations,
                GatewayAuthorizationProvider::providerId,
                "authorization provider"
        );
        this.identityMappers = index(
                identityMappers,
                GatewayIdentityMapper::mapperId,
                "identity mapper"
        );
        this.recoveries = index(
                recoveries,
                GatewayCredentialRecoveryProvider::providerId,
                "credential recovery provider"
        );
    }

    /**
     * Compatibility constructor for applications without a recovery provider.
     */
    public GatewaySecurityCapabilityRegistry(
            Collection<GatewayCredentialExtractor> extractors,
            Collection<GatewayAuthenticationProvider> authentications,
            Collection<GatewayAuthorizationProvider> authorizations,
            Collection<GatewayIdentityMapper> identityMappers) {
        this(extractors, authentications, authorizations, identityMappers, Set.of());
    }

    /**
     * 中文说明：执行 empty 操作；该方法是 {@code GatewaySecurityCapabilityRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the empty operation; this method is the invocation entry point on {@code GatewaySecurityCapabilityRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityCapabilityRegistry.empty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 empty 的处理结果；returns the result of the operation.
     */
    public static GatewaySecurityCapabilityRegistry empty() {
        return new GatewaySecurityCapabilityRegistry(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewaySecurityCapabilityRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewaySecurityCapabilityRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityCapabilityRegistry.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param protocols 参数 protocols；parameter protocols。
     */
    public void validate(
            GatewaySecurityPolicy policy,
            Set<GatewayProtocol> protocols) {
        Set<String> credentialTypes = policy.credentialExtractorIds().stream()
                .map(id -> extractor(id).credentialType())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (String id : policy.authenticationProviderIds()) {
            GatewayAuthenticationProvider provider = authentication(id);
            if (provider.supportedCredentialTypes().stream()
                    .noneMatch(credentialTypes::contains)) {
                throw new IllegalArgumentException(
                        "authentication provider "
                                + id
                                + " does not support extracted credentials"
                );
            }
        }
        policy.authorizationProviderIds().forEach(this::authorization);
        if (policy.identityMapperId() != null) {
            GatewayIdentityMapper mapper = identityMapper(
                    policy.identityMapperId()
            );
            if (!mapper.supportedProtocols().containsAll(protocols)) {
                throw new IllegalArgumentException(
                        "identity mapper "
                                + mapper.mapperId()
                                + " does not support "
                                + protocols
                );
            }
        }
        if (policy.credentialRecoveryProviderId() != null) {
            recovery(policy.credentialRecoveryProviderId());
        }
    }

    /**
     * 中文说明：执行 extractor 操作；该方法是 {@code GatewaySecurityCapabilityRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the extractor operation; this method is the invocation entry point on {@code GatewaySecurityCapabilityRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityCapabilityRegistry.extractor(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 extractor 的处理结果；returns the result of the operation.
     */
    public GatewayCredentialExtractor extractor(String id) {
        return required(extractors, id, "credential extractor");
    }

    /**
     * 中文说明：执行 authentication 操作；该方法是 {@code GatewaySecurityCapabilityRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authentication operation; this method is the invocation entry point on {@code GatewaySecurityCapabilityRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityCapabilityRegistry.authentication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 authentication 的处理结果；returns the result of the operation.
     */
    public GatewayAuthenticationProvider authentication(String id) {
        return required(authentications, id, "authentication provider");
    }

    /**
     * 中文说明：执行 授权 操作；该方法是 {@code GatewaySecurityCapabilityRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorization operation; this method is the invocation entry point on {@code GatewaySecurityCapabilityRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityCapabilityRegistry.authorization(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 授权 的处理结果；returns the result of the operation.
     */
    public GatewayAuthorizationProvider authorization(String id) {
        return required(authorizations, id, "authorization provider");
    }

    /**
     * 中文说明：执行 身份映射器 操作；该方法是 {@code GatewaySecurityCapabilityRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity mapper operation; this method is the invocation entry point on {@code GatewaySecurityCapabilityRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityCapabilityRegistry.identityMapper(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 身份映射器 的处理结果；returns the result of the operation.
     */
    public GatewayIdentityMapper identityMapper(String id) {
        return required(identityMappers, id, "identity mapper");
    }

    public GatewayCredentialRecoveryProvider recovery(String id) {
        return required(recoveries, id, "credential recovery provider");
    }

    /**
     * 中文说明：执行 索引 操作；该方法是 {@code GatewaySecurityCapabilityRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the index operation; this method is the invocation entry point on {@code GatewaySecurityCapabilityRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityCapabilityRegistry.index(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param identifier 参数 identifier；parameter identifier。
     * @param type 参数 type；parameter type。
     * @return 返回 索引 的处理结果；returns the result of the operation.
     */
    private <T> Map<String, T> index(
            Collection<T> values,
            Function<T, String> identifier,
            String type) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T value : values) {
            String id = identifier.apply(value);
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(type + " id is required");
            }
            if (result.putIfAbsent(id.trim(), value) != null) {
                throw new IllegalArgumentException(
                        "duplicate " + type + " id " + id
                );
            }
        }
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewaySecurityCapabilityRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewaySecurityCapabilityRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityCapabilityRegistry.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param id 参数 id；parameter id。
     * @param type 参数 type；parameter type。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private <T> T required(Map<String, T> values, String id, String type) {
        T value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException(
                    "missing " + type + " " + id
            );
        }
        return value;
    }
}
