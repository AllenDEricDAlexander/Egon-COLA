package top.egon.cola.component.gateway.engine.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;

import java.util.Map;
import java.util.Objects;

/**
 * Resolves reviewed auth profiles without accepting an inbound bearer token.
 * 补充说明 / Supplementary summary: {@code ReferenceRemoteAuthProvider} 是提供方组件，位于当前 Gateway 模块的相关包中，负责Reference远程认证提供方相关的职责与边界。
 * English supplement: {@code ReferenceRemoteAuthProvider} is a reference remote auth provider provider in the current Gateway module; it owns the reference remote auth provider-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ReferenceRemoteAuthProvider
        implements RemoteAuthProvider {

    /**
     * 中文说明：保存 profiles 对应的状态、依赖或配置值；字段类型为 {@code ProfileResolver}，由 {@code ReferenceRemoteAuthProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by profiles; its type is {@code ProfileResolver}, and {@code ReferenceRemoteAuthProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProfileResolver profiles;

    /**
     * 中文说明：保存 oauth 对应的状态、依赖或配置值；字段类型为 {@code OAuthTokenClient}，由 {@code ReferenceRemoteAuthProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by oauth; its type is {@code OAuthTokenClient}, and {@code ReferenceRemoteAuthProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final OAuthTokenClient oauth;

    /**
     * 中文说明：创建 {@code ReferenceRemoteAuthProvider} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ReferenceRemoteAuthProvider} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param profiles 参数 profiles；parameter profiles。
     * @param oauth 参数 oauth；parameter oauth。
     */
    public ReferenceRemoteAuthProvider(
            ProfileResolver profiles,
            OAuthTokenClient oauth) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.oauth = Objects.requireNonNull(oauth, "oauth");
    }

    /**
     * 中文说明：执行 resolve 操作；该方法是 {@code ReferenceRemoteAuthProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve operation; this method is the invocation entry point on {@code ReferenceRemoteAuthProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReferenceRemoteAuthProvider.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 resolve 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<OutboundAuthentication> resolve(AuthRequest request) {
        String reference = request.provider().authProfileReference();
        if (reference == null) {
            return Mono.just(new OutboundAuthentication(
                    Map.of(),
                    request.provider().tlsProfileReference()
            ));
        }
        return Mono.from(profiles.resolve(reference, request.context()))
                .flatMap(profile -> switch (profile.type()) {
                    case SECRET_REFERENCE -> Mono.just(
                            authentication(
                                    profile.authorization(),
                                    profile.tlsProfileReference()
                            )
                    );
                    case OAUTH_CLIENT_CREDENTIALS -> token(
                            profile,
                            request.context(),
                            "client_credentials",
                            null
                    );
                    case TOKEN_EXCHANGE -> token(
                            profile,
                            request.context(),
                            "urn:ietf:params:oauth:grant-type:token-exchange",
                            profile.subjectTokenReference()
                    );
                    case MTLS -> Mono.just(new OutboundAuthentication(
                            Map.of(),
                            required(
                                    profile.tlsProfileReference(),
                                    "tlsProfileReference"
                            )
                    ));
                });
    }

    /**
     * 中文说明：执行 token 操作；该方法是 {@code ReferenceRemoteAuthProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the token operation; this method is the invocation entry point on {@code ReferenceRemoteAuthProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReferenceRemoteAuthProvider.token(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param profile 参数 profile；parameter profile。
     * @param context 参数 context；parameter context。
     * @param grantType 参数 grantType；parameter grant type。
     * @param subjectTokenReference 参数 subjectTokenReference；parameter subject token reference。
     * @return 返回 token 的处理结果；returns the result of the operation.
     */
    private Mono<OutboundAuthentication> token(
            Profile profile,
            AuthContext context,
            String grantType,
            String subjectTokenReference) {
        OAuthTokenRequest request = new OAuthTokenRequest(
                required(profile.tokenEndpoint(), "tokenEndpoint"),
                required(profile.clientId(), "clientId"),
                required(
                        profile.clientSecretReference(),
                        "clientSecretReference"
                ),
                grantType,
                subjectTokenReference,
                profile.scope(),
                context,
                profile.tlsProfileReference()
        );
        return Mono.from(oauth.acquire(request))
                .map(token -> authentication(
                        token.tokenType() + " " + token.accessToken(),
                        profile.tlsProfileReference()
                ));
    }

    /**
     * 中文说明：执行 authentication 操作；该方法是 {@code ReferenceRemoteAuthProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authentication operation; this method is the invocation entry point on {@code ReferenceRemoteAuthProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReferenceRemoteAuthProvider.authentication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authorization 参数 授权；parameter authorization。
     * @param tlsProfileReference 参数 tlsProfileReference；parameter tls profile reference。
     * @return 返回 authentication 的处理结果；returns the result of the operation.
     */
    private OutboundAuthentication authentication(
            String authorization,
            String tlsProfileReference) {
        return new OutboundAuthentication(
                Map.of("authorization", required(
                        authorization,
                        "authorization"
                )),
                tlsProfileReference
        );
    }

    /**
     * 中文说明：{@code ProfileResolver} 是接口契约，位于当前 Gateway 模块的相关包中，负责ProfileResolver相关的职责与边界。
     * English summary: {@code ProfileResolver} is an interface contract in the current Gateway module; it owns the profile resolver-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface ProfileResolver {

        /**
         * 中文说明：执行 resolve 操作；该方法是 {@code ReferenceRemoteAuthProvider.ProfileResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the resolve operation; this method is the invocation entry point on {@code ReferenceRemoteAuthProvider.ProfileResolver} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReferenceRemoteAuthProvider.ProfileResolver.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param profileReference 参数 profileReference；parameter profile reference。
         * @param context 参数 context；parameter context。
         * @return 返回 resolve 的处理结果；returns the result of the operation.
         */
        Publisher<Profile> resolve(
                String profileReference,
                AuthContext context
        );
    }

    /**
     * 中文说明：{@code OAuthTokenClient} 是接口契约，位于当前 Gateway 模块的相关包中，负责O认证Token客户端相关的职责与边界。
     * English summary: {@code OAuthTokenClient} is an interface contract in the current Gateway module; it owns the o auth token client-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface OAuthTokenClient {

        /**
         * 中文说明：执行 acquire 操作；该方法是 {@code ReferenceRemoteAuthProvider.OAuthTokenClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the acquire operation; this method is the invocation entry point on {@code ReferenceRemoteAuthProvider.OAuthTokenClient} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReferenceRemoteAuthProvider.OAuthTokenClient.acquire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param request 参数 请求；parameter request。
         * @return 返回 acquire 的处理结果；returns the result of the operation.
         */
        Publisher<OAuthToken> acquire(OAuthTokenRequest request);
    }

    /**
     * 中文说明：{@code ProfileType} 是枚举类型，位于当前 Gateway 模块的相关包中，负责ProfileType相关的职责与边界。
     * English summary: {@code ProfileType} is an enumeration in the current Gateway module; it owns the profile type-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public enum ProfileType {
        /**
         * 中文说明：表示 SECRETREFERENCE 这一固定值；它属于 {@code ReferenceRemoteAuthProvider.ProfileType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value secret reference; it is a state, type, or protocol value of {@code ReferenceRemoteAuthProvider.ProfileType} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.ProfileType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.ProfileType}; do not couple callers to its representation when the owning type exposes an API.
         */
        SECRET_REFERENCE,
        /**
         * 中文说明：表示 OAUTH客户端CREDENTIALS 这一固定值；它属于 {@code ReferenceRemoteAuthProvider.ProfileType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value oauth client credentials; it is a state, type, or protocol value of {@code ReferenceRemoteAuthProvider.ProfileType} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.ProfileType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.ProfileType}; do not couple callers to its representation when the owning type exposes an API.
         */
        OAUTH_CLIENT_CREDENTIALS,
        /**
         * 中文说明：表示 TOKENEXCHANGE 这一固定值；它属于 {@code ReferenceRemoteAuthProvider.ProfileType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value token exchange; it is a state, type, or protocol value of {@code ReferenceRemoteAuthProvider.ProfileType} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.ProfileType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.ProfileType}; do not couple callers to its representation when the owning type exposes an API.
         */
        TOKEN_EXCHANGE,
        /**
         * 中文说明：表示 MTLS 这一固定值；它属于 {@code ReferenceRemoteAuthProvider.ProfileType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value mtls; it is a state, type, or protocol value of {@code ReferenceRemoteAuthProvider.ProfileType} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.ProfileType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.ProfileType}; do not couple callers to its representation when the owning type exposes an API.
         */
        MTLS
    }

    /**
     * 中文说明：{@code Profile} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Profile相关的职责与边界。
     * English summary: {@code Profile} is an immutable data carrier in the current Gateway module; it owns the profile-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param type 参数 type；parameter type。
     * @param authorization 参数 授权；parameter authorization。
     * @param tokenEndpoint 参数 tokenEndpoint；parameter token endpoint。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param clientSecretReference 参数 客户端SecretReference；parameter client secret reference。
     * @param subjectTokenReference 参数 subjectTokenReference；parameter subject token reference。
     * @param scope 参数 scope；parameter scope。
     * @param tlsProfileReference 参数 tlsProfileReference；parameter tls profile reference。
     */
    public record Profile(
            /**
             * 中文说明：保存 type 对应的状态、依赖或配置值；字段类型为 {@code ProfileType}，由 {@code ReferenceRemoteAuthProvider.Profile} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by type; its type is {@code ProfileType}, and {@code ReferenceRemoteAuthProvider.Profile} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.Profile} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.Profile}; do not couple callers to its representation when the owning type exposes an API.
             */
            ProfileType type,
            /**
             * 中文说明：保存 授权 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.Profile} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by authorization; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.Profile} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.Profile} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.Profile}; do not couple callers to its representation when the owning type exposes an API.
             */
            String authorization,
            /**
             * 中文说明：保存 tokenEndpoint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.Profile} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by token endpoint; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.Profile} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.Profile} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.Profile}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tokenEndpoint,
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.Profile} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.Profile} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.Profile} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.Profile}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId,
            /**
             * 中文说明：保存 客户端SecretReference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.Profile} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client secret reference; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.Profile} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.Profile} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.Profile}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientSecretReference,
            /**
             * 中文说明：保存 subjectTokenReference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.Profile} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subject token reference; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.Profile} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.Profile} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.Profile}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subjectTokenReference,
            /**
             * 中文说明：保存 scope 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.Profile} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by scope; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.Profile} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.Profile} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.Profile}; do not couple callers to its representation when the owning type exposes an API.
             */
            String scope,
            /**
             * 中文说明：保存 tlsProfileReference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.Profile} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tls profile reference; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.Profile} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.Profile} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.Profile}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tlsProfileReference
    ) {

        /**
         * 中文说明：创建 {@code ReferenceRemoteAuthProvider.Profile} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code ReferenceRemoteAuthProvider.Profile} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param type 参数 type；parameter type。
         * @param authorization 参数 授权；parameter authorization。
         * @param tokenEndpoint 参数 tokenEndpoint；parameter token endpoint。
         * @param clientId 参数 客户端Id；parameter client id。
         * @param clientSecretReference 参数 客户端SecretReference；parameter client secret reference。
         * @param subjectTokenReference 参数 subjectTokenReference；parameter subject token reference。
         * @param scope 参数 scope；parameter scope。
         * @param tlsProfileReference 参数 tlsProfileReference；parameter tls profile reference。
         */
        public Profile {
            type = Objects.requireNonNull(type, "type");
            authorization = optional(authorization);
            tokenEndpoint = optional(tokenEndpoint);
            clientId = optional(clientId);
            clientSecretReference = optional(clientSecretReference);
            subjectTokenReference = optional(subjectTokenReference);
            scope = optional(scope);
            tlsProfileReference = optional(tlsProfileReference);
        }

        /**
         * 中文说明：执行 secretReference 操作；该方法是 {@code ReferenceRemoteAuthProvider.Profile} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the secret reference operation; this method is the invocation entry point on {@code ReferenceRemoteAuthProvider.Profile} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReferenceRemoteAuthProvider.Profile.secretReference(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param authorization 参数 授权；parameter authorization。
         * @return 返回 secretReference 的处理结果；returns the result of the operation.
         */
        public static Profile secretReference(String authorization) {
            return new Profile(
                    ProfileType.SECRET_REFERENCE,
                    authorization,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    /**
     * 中文说明：{@code OAuthTokenRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责O认证Token请求相关的职责与边界。
     * English summary: {@code OAuthTokenRequest} is an immutable data carrier in the current Gateway module; it owns the o auth token request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param tokenEndpoint 参数 tokenEndpoint；parameter token endpoint。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param clientSecretReference 参数 客户端SecretReference；parameter client secret reference。
     * @param grantType 参数 grantType；parameter grant type。
     * @param subjectTokenReference 参数 subjectTokenReference；parameter subject token reference。
     * @param scope 参数 scope；parameter scope。
     * @param context 参数 context；parameter context。
     * @param tlsProfileReference 参数 tlsProfileReference；parameter tls profile reference。
     */
    public record OAuthTokenRequest(
            /**
             * 中文说明：保存 tokenEndpoint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by token endpoint; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tokenEndpoint,
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId,
            /**
             * 中文说明：保存 客户端SecretReference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client secret reference; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientSecretReference,
            /**
             * 中文说明：保存 grantType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by grant type; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String grantType,
            /**
             * 中文说明：保存 subjectTokenReference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subject token reference; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subjectTokenReference,
            /**
             * 中文说明：保存 scope 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by scope; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String scope,
            /**
             * 中文说明：保存 context 对应的状态、依赖或配置值；字段类型为 {@code AuthContext}，由 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by context; its type is {@code AuthContext}, and {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            AuthContext context,
            /**
             * 中文说明：保存 tlsProfileReference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tls profile reference; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tlsProfileReference
    ) {

        /**
         * 中文说明：创建 {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code ReferenceRemoteAuthProvider.OAuthTokenRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param tokenEndpoint 参数 tokenEndpoint；parameter token endpoint。
         * @param clientId 参数 客户端Id；parameter client id。
         * @param clientSecretReference 参数 客户端SecretReference；parameter client secret reference。
         * @param grantType 参数 grantType；parameter grant type。
         * @param subjectTokenReference 参数 subjectTokenReference；parameter subject token reference。
         * @param scope 参数 scope；parameter scope。
         * @param context 参数 context；parameter context。
         * @param tlsProfileReference 参数 tlsProfileReference；parameter tls profile reference。
         */
        public OAuthTokenRequest {
            tokenEndpoint = required(tokenEndpoint, "tokenEndpoint");
            clientId = required(clientId, "clientId");
            clientSecretReference = required(
                    clientSecretReference,
                    "clientSecretReference"
            );
            grantType = required(grantType, "grantType");
            subjectTokenReference = optional(subjectTokenReference);
            scope = optional(scope);
            context = Objects.requireNonNull(context, "context");
            tlsProfileReference = optional(tlsProfileReference);
        }
    }

    /**
     * 中文说明：{@code OAuthToken} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责O认证Token相关的职责与边界。
     * English summary: {@code OAuthToken} is an immutable data carrier in the current Gateway module; it owns the o auth token-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param accessToken 参数 accessToken；parameter access token。
     * @param tokenType 参数 tokenType；parameter token type。
     */
    public record OAuthToken(
    /**
     * 中文说明：保存 accessToken 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthToken} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by access token; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthToken} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthToken} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthToken}; do not couple callers to its representation when the owning type exposes an API.
     */
    String accessToken,
    /**
     * 中文说明：保存 tokenType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReferenceRemoteAuthProvider.OAuthToken} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by token type; its type is {@code String}, and {@code ReferenceRemoteAuthProvider.OAuthToken} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReferenceRemoteAuthProvider.OAuthToken} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReferenceRemoteAuthProvider.OAuthToken}; do not couple callers to its representation when the owning type exposes an API.
     */
    String tokenType) {

        /**
         * 中文说明：创建 {@code ReferenceRemoteAuthProvider.OAuthToken} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code ReferenceRemoteAuthProvider.OAuthToken} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param accessToken 参数 accessToken；parameter access token。
         * @param tokenType 参数 tokenType；parameter token type。
         */
        public OAuthToken {
            accessToken = required(accessToken, "accessToken");
            tokenType = tokenType == null || tokenType.isBlank()
                    ? "Bearer"
                    : tokenType.trim();
        }
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code ReferenceRemoteAuthProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code ReferenceRemoteAuthProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReferenceRemoteAuthProvider.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "remote auth profile " + field + " is required"
            );
        }
        return value.trim();
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code ReferenceRemoteAuthProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code ReferenceRemoteAuthProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReferenceRemoteAuthProvider.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
