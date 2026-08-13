package top.egon.cola.platform.rbac3.admin.integration.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 类型 `GatewayAdminControlPlaneStatusClient` 位于当前包内，是类型，用于承载 `Gateway Admin Control Plane Status Client` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `GatewayAdminControlPlaneStatusClient` is a type in its package and carries the responsibility, state, or contract for `Gateway Admin Control Plane Status Client`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Read-only typed client for Gateway Admin release and discovery observations.
 */
public final class GatewayAdminControlPlaneStatusClient {

    /**
     * 字段 `adminBaseUri` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `admin Base Uri` 相关的状态、依赖、配置或结果（声明类型 `URI`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `adminBaseUri` stores the `admin Base Uri`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `URI`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `adminBaseUri` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `adminBaseUri`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final URI adminBaseUri;
    /**
     * 字段 `gatewayGroupId` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `gateway Group Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `gatewayGroupId` stores the `gateway Group Id`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `gatewayGroupId` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `gatewayGroupId`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String gatewayGroupId;
    /**
     * 字段 `releaseId` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `release Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `releaseId` stores the `release Id`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `releaseId` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `releaseId`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String releaseId;
    /**
     * 字段 `providerServiceKey` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `provider Service Key` 相关的状态、依赖、配置或结果（声明类型 `ServiceKey`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `providerServiceKey` stores the `provider Service Key`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `ServiceKey`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `providerServiceKey` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `providerServiceKey`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ServiceKey providerServiceKey;
    /**
     * 字段 `credentials` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `credentials` 相关的状态、依赖、配置或结果（声明类型 `GatewayAdminStatusCredentialProvider`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentials` stores the `credentials`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `GatewayAdminStatusCredentialProvider`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentials` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentials`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final GatewayAdminStatusCredentialProvider credentials;
    /**
     * 字段 `transport` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `transport` 相关的状态、依赖、配置或结果（声明类型 `Transport`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `transport` stores the `transport`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `Transport`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `transport` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `transport`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Transport transport;
    /**
     * 字段 `objectMapper` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;
    /**
     * 字段 `clock` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;
    /**
     * 字段 `timeout` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `timeout` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `timeout` stores the `timeout`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `timeout` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `timeout`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration timeout;

    /**
     * 构造器 `GatewayAdminControlPlaneStatusClient` 用于创建并初始化 `GatewayAdminControlPlaneStatusClient` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayAdminControlPlaneStatusClient` creates and initializes `GatewayAdminControlPlaneStatusClient`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayAdminControlPlaneStatusClient` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayAdminControlPlaneStatusClient`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param adminBaseUri 输入参数 `adminBaseUri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gatewayGroupId 输入参数 `gatewayGroupId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param releaseId 输入参数 `releaseId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentials 输入参数 `credentials`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayAdminControlPlaneStatusClient(
            URI adminBaseUri,
            String gatewayGroupId,
            String releaseId,
            GatewayAdminStatusCredentialProvider credentials,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this(adminBaseUri, gatewayGroupId, releaseId, new ServiceKey(
                        "rbac3", "rbac3-admin", "prod", "default",
                        "HTTP_PROVIDER", "http",
                        "rbac3-admin", "default", "1.0.0"),
                credentials, new JdkTransport(timeout), objectMapper, clock, timeout);
    }

    /**
     * 构造器 `GatewayAdminControlPlaneStatusClient` 用于创建并初始化 `GatewayAdminControlPlaneStatusClient` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayAdminControlPlaneStatusClient` creates and initializes `GatewayAdminControlPlaneStatusClient`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayAdminControlPlaneStatusClient` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayAdminControlPlaneStatusClient`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param adminBaseUri 输入参数 `adminBaseUri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gatewayGroupId 输入参数 `gatewayGroupId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param releaseId 输入参数 `releaseId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerServiceKey 输入参数 `providerServiceKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentials 输入参数 `credentials`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayAdminControlPlaneStatusClient(
            URI adminBaseUri,
            String gatewayGroupId,
            String releaseId,
            ServiceKey providerServiceKey,
            GatewayAdminStatusCredentialProvider credentials,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this(adminBaseUri, gatewayGroupId, releaseId, providerServiceKey,
                credentials, new JdkTransport(timeout), objectMapper, clock, timeout);
    }

    /**
     * 构造器 `GatewayAdminControlPlaneStatusClient` 用于创建并初始化 `GatewayAdminControlPlaneStatusClient` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayAdminControlPlaneStatusClient` creates and initializes `GatewayAdminControlPlaneStatusClient`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayAdminControlPlaneStatusClient` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayAdminControlPlaneStatusClient`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param adminBaseUri 输入参数 `adminBaseUri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gatewayGroupId 输入参数 `gatewayGroupId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param releaseId 输入参数 `releaseId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentials 输入参数 `credentials`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transport 输入参数 `transport`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayAdminControlPlaneStatusClient(
            URI adminBaseUri,
            String gatewayGroupId,
            String releaseId,
            GatewayAdminStatusCredentialProvider credentials,
            Transport transport,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this(adminBaseUri, gatewayGroupId, releaseId, new ServiceKey(
                        "rbac3", "rbac3-admin", "prod", "default",
                        "HTTP_PROVIDER", "http",
                        "rbac3-admin", "default", "1.0.0"),
                credentials, transport, objectMapper, clock, timeout);
    }

    /**
     * 构造器 `GatewayAdminControlPlaneStatusClient` 用于创建并初始化 `GatewayAdminControlPlaneStatusClient` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `GatewayAdminControlPlaneStatusClient` creates and initializes `GatewayAdminControlPlaneStatusClient`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `GatewayAdminControlPlaneStatusClient` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `GatewayAdminControlPlaneStatusClient`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param adminBaseUri 输入参数 `adminBaseUri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gatewayGroupId 输入参数 `gatewayGroupId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param releaseId 输入参数 `releaseId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerServiceKey 输入参数 `providerServiceKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentials 输入参数 `credentials`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transport 输入参数 `transport`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public GatewayAdminControlPlaneStatusClient(
            URI adminBaseUri,
            String gatewayGroupId,
            String releaseId,
            ServiceKey providerServiceKey,
            GatewayAdminStatusCredentialProvider credentials,
            Transport transport,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this.adminBaseUri = root(adminBaseUri);
        this.gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
        this.releaseId = required(releaseId, "releaseId");
        this.providerServiceKey = Objects.requireNonNull(
                providerServiceKey, "providerServiceKey").validated();
        this.credentials = Objects.requireNonNull(credentials, "credentials");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.timeout = positive(timeout);
    }

    /**
     * 方法 `snapshot` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `snapshot` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `snapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `snapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public GatewayAdminSnapshot snapshot() {
        Instant checkedAt = clock.instant();
        Optional<GatewayAdminStatusCredentialProvider.BearerCredential> supplied =
                credentials.current();
        if (supplied.isEmpty()) {
            return unknown("CREDENTIAL_MISSING", checkedAt);
        }
        var credential = supplied.orElseThrow();
        if (!credential.expiresAt().isAfter(checkedAt)) {
            return unknown("CREDENTIAL_EXPIRED", checkedAt);
        }
        return new GatewayAdminSnapshot(
                release(credential.accessToken()),
                providers(credential.accessToken()),
                consistency(credential.accessToken()),
                checkedAt);
    }

    /**
     * 方法 `release` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `release` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `release` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `release` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `release` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `release`, then continue the business flow using its result, exception, or side effect.
     *
     * @param token 输入参数 `token`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ReleaseObservation release(String token) {
        Response response = get("/api/v1/gateway/admin/releases/" + encode(releaseId), token);
        if (!response.success()) {
            return ReleaseObservation.unknown(releaseId, response.reasonCode());
        }
        JsonNode json = response.json();
        return new ReleaseObservation(
                "SUCCESS", text(json, "releaseId"), text(json, "status"),
                recursiveText(json, "definitionSetId"),
                recursiveText(json, "publishedVersion"), null);
    }

    /**
     * 方法 `providers` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `providers` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `providers` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `providers` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `providers` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `providers`, then continue the business flow using its result, exception, or side effect.
     *
     * @param token 输入参数 `token`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ProviderObservation providers(String token) {
        String query = "?bizCode=" + encode(providerServiceKey.bizCode())
                + "&appCode=" + encode(providerServiceKey.appCode())
                + "&env=" + encode(providerServiceKey.env())
                + "&namespace=" + encode(providerServiceKey.namespace())
                + "&serviceKind=" + encode(providerServiceKey.serviceKind())
                + "&protocol=" + encode(providerServiceKey.protocol())
                + "&serviceName=" + encode(providerServiceKey.serviceName())
                + "&group=" + encode(providerServiceKey.group())
                + "&version=" + encode(providerServiceKey.version());
        Response response = get(
                "/api/v1/gateway/admin/providers/instances" + query, token);
        if (!response.success()) {
            return ProviderObservation.unknown(response.reasonCode());
        }
        List<ProviderInstance> instances = new ArrayList<>();
        collectInstances(response.json(), instances);
        return new ProviderObservation("SUCCESS", instances, null);
    }

    /**
     * 方法 `consistency` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `consistency` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `consistency` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `consistency` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `consistency` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `consistency`, then continue the business flow using its result, exception, or side effect.
     *
     * @param token 输入参数 `token`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ConsistencyObservation consistency(String token) {
        Response response = get(
                "/api/v1/gateway/admin/gateway-groups/" + encode(gatewayGroupId)
                        + "/runtime-consistency", token);
        if (!response.success()) {
            return ConsistencyObservation.unknown(response.reasonCode());
        }
        JsonNode json = response.json();
        return new ConsistencyObservation(
                "SUCCESS", text(json, "releaseId"),
                firstText(json, "releaseStatus", "status"),
                json.path("consistent").asBoolean(false),
                recursiveText(json, "activeRuleVersion"), null);
    }

    /**
     * 方法 `get` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `get` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
     *
     * @param path 输入参数 `path`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param token 输入参数 `token`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Response get(String path, String token) {
        try {
            HttpResponse result = transport.get(adminBaseUri.resolve(path), token, timeout);
            if (result.statusCode() == 403) {
                return Response.failure("GATEWAY_STATUS_FORBIDDEN");
            }
            if (result.statusCode() < 200 || result.statusCode() >= 300) {
                return Response.failure("GATEWAY_STATUS_UNAVAILABLE");
            }
            return Response.success(objectMapper.readTree(result.body()));
        } catch (Exception unavailable) {
            return Response.failure("GATEWAY_STATUS_UNAVAILABLE");
        }
    }

    /**
     * 方法 `collectInstances` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `collect Instances` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `collectInstances` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `collect Instances` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `collectInstances` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `collectInstances`, then continue the business flow using its result, exception, or side effect.
     *
     * @param node 输入参数 `node`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param target 输入参数 `target`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void collectInstances(JsonNode node, List<ProviderInstance> target) {
        if (node.isArray()) {
            node.forEach(value -> collectInstances(value, target));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode serviceKey = node.get("serviceKey");
        if (serviceKey != null && serviceKey.isObject() && node.has("instanceId")) {
            target.add(new ProviderInstance(
                    text(node, "instanceId"), text(node, "status"),
                    new ServiceKey(
                            text(serviceKey, "bizCode"), text(serviceKey, "appCode"),
                            text(serviceKey, "env"), text(serviceKey, "namespace"),
                            text(serviceKey, "serviceKind"), text(serviceKey, "protocol"),
                            text(serviceKey, "serviceName"), text(serviceKey, "group"),
                            firstText(serviceKey, "version", "artifactVersion")),
                    recursiveText(node.path("metadata"), "gateway.definition-set-id")));
            return;
        }
        node.elements().forEachRemaining(value -> collectInstances(value, target));
    }

    /**
     * 方法 `unknown` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `unknown` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `unknown` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `unknown` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `unknown` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `unknown`, then continue the business flow using its result, exception, or side effect.
     *
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param checkedAt 输入参数 `checkedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private GatewayAdminSnapshot unknown(String reasonCode, Instant checkedAt) {
        return new GatewayAdminSnapshot(
                ReleaseObservation.unknown(releaseId, reasonCode),
                ProviderObservation.unknown(reasonCode),
                ConsistencyObservation.unknown(reasonCode),
                checkedAt);
    }

    /**
     * 方法 `recursiveText` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `recursive Text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recursiveText` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `recursive Text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recursiveText` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recursiveText`, then continue the business flow using its result, exception, or side effect.
     *
     * @param node 输入参数 `node`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String recursiveText(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode direct = node.get(field);
            if (direct != null && direct.isValueNode() && !direct.asText().isBlank()) {
                return direct.asText();
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                String nested = recursiveText(fields.next().getValue(), field);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode value : node) {
                String nested = recursiveText(value, field);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /**
     * 方法 `text` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `text` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `text` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `text`, then continue the business flow using its result, exception, or side effect.
     *
     * @param node 输入参数 `node`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * 方法 `firstText` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `first Text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `firstText` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `first Text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `firstText` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `firstText`, then continue the business flow using its result, exception, or side effect.
     *
     * @param node 输入参数 `node`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fields 输入参数 `fields`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 方法 `root` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `root` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `root` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `root` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `root` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `root`, then continue the business flow using its result, exception, or side effect.
     *
     * @param uri 输入参数 `uri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static URI root(URI uri) {
        Objects.requireNonNull(uri, "adminBaseUri");
        if (!("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getQuery() != null
                || uri.getFragment() != null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Gateway Admin base URI is invalid");
        }
        String value = uri.toString();
        return URI.create(value.endsWith("/") ? value : value + '/');
    }

    /**
     * 方法 `positive` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `positive` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `positive` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `positive` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `positive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `positive`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Duration positive(Duration value) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        return value;
    }

    /**
     * 方法 `required` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 方法 `encode` 按照 `GatewayAdminControlPlaneStatusClient` 的职责处理输入，完成 `encode` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `encode` processes its inputs according to `GatewayAdminControlPlaneStatusClient`'s responsibility, performs the `encode` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `encode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `encode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 类型 `Transport` 位于 `GatewayAdminControlPlaneStatusClient` 内，是接口，用于承载 `Transport` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Transport` is an interface inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Transport`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Transport` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Transport` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface Transport {

        /**
         * 方法 `get` 按照 `Transport` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `get` processes its inputs according to `Transport`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
         *
         * @param uri 输入参数 `uri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param bearerToken 输入参数 `bearerToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         * @throws IOException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         * @throws InterruptedException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         */
        HttpResponse get(URI uri, String bearerToken, Duration timeout)
                throws IOException, InterruptedException;
    }

    /**
     * 类型 `HttpResponse` 位于 `GatewayAdminControlPlaneStatusClient` 内，是记录类型，用于承载 `Http Response` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `HttpResponse` is a record inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Http Response`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `HttpResponse` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `HttpResponse` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param statusCode 记录组件 `statusCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `statusCode` carries constructor data whose meaning is defined by the record contract.
     * @param body 记录组件 `body` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `body` carries constructor data whose meaning is defined by the record contract.
     */
    public record HttpResponse(/**
 * 字段 `statusCode` 表示 `HttpResponse` 中与 `status Code` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `statusCode` stores the `status Code`-related state, dependency, configuration, or result of `HttpResponse` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `statusCode` 时应保持 `HttpResponse` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `statusCode`, preserve `HttpResponse`'s lifecycle, immutability, and thread-safety constraints.
 */ int statusCode, /**
 * 字段 `body` 表示 `HttpResponse` 中与 `body` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `body` stores the `body`-related state, dependency, configuration, or result of `HttpResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `body` 时应保持 `HttpResponse` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `body`, preserve `HttpResponse`'s lifecycle, immutability, and thread-safety constraints.
 */ String body) {
    }

    /**
     * 类型 `JdkTransport` 位于 `GatewayAdminControlPlaneStatusClient` 内，是类型，用于承载 `Jdk Transport` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `JdkTransport` is a type inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Jdk Transport`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `JdkTransport` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `JdkTransport` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    private static final class JdkTransport implements Transport {

        /**
         * 字段 `client` 表示 `JdkTransport` 中与 `client` 相关的状态、依赖、配置或结果（声明类型 `HttpClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `client` stores the `client`-related state, dependency, configuration, or result of `JdkTransport` (declared type `HttpClient`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `client` 时应保持 `JdkTransport` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `client`, preserve `JdkTransport`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final HttpClient client;

        /**
         * 构造器 `JdkTransport` 用于创建并初始化 `JdkTransport` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `JdkTransport` creates and initializes `JdkTransport`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `JdkTransport` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `JdkTransport`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param connectTimeout 输入参数 `connectTimeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        private JdkTransport(Duration connectTimeout) {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
        }

        /**
         * 方法 `get` 按照 `JdkTransport` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `get` processes its inputs according to `JdkTransport`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
         *
         * @param uri 输入参数 `uri`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param bearerToken 输入参数 `bearerToken`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param timeout 输入参数 `timeout`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         * @throws IOException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         * @throws InterruptedException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         */
        @Override
        public HttpResponse get(URI uri, String bearerToken, Duration timeout)
                throws IOException, InterruptedException {
            var request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .GET()
                    .build();
            var response = client.send(request, BodyHandlers.ofString());
            return new HttpResponse(response.statusCode(), response.body());
        }
    }

    /**
     * 类型 `Response` 位于 `GatewayAdminControlPlaneStatusClient` 内，是记录类型，用于承载 `Response` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Response` is a record inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Response`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Response` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Response` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param json 记录组件 `json` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `json` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    private record Response(/**
 * 字段 `json` 表示 `Response` 中与 `json` 相关的状态、依赖、配置或结果（声明类型 `JsonNode`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `json` stores the `json`-related state, dependency, configuration, or result of `Response` (declared type `JsonNode`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `json` 时应保持 `Response` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `json`, preserve `Response`'s lifecycle, immutability, and thread-safety constraints.
 */ JsonNode json, /**
 * 字段 `reasonCode` 表示 `Response` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `Response` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `Response` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `Response`'s lifecycle, immutability, and thread-safety constraints.
 */ String reasonCode) {

        /**
         * 方法 `success` 按照 `Response` 的职责处理输入，完成 `success` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `success` processes its inputs according to `Response`'s responsibility, performs the `success` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `success` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `success`, then continue the business flow using its result, exception, or side effect.
         *
         * @param json 输入参数 `json`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        static Response success(JsonNode json) {
            return new Response(json, null);
        }

        /**
         * 方法 `failure` 按照 `Response` 的职责处理输入，完成 `failure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `failure` processes its inputs according to `Response`'s responsibility, performs the `failure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `failure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `failure`, then continue the business flow using its result, exception, or side effect.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        static Response failure(String reasonCode) {
            return new Response(null, reasonCode);
        }

        /**
         * 方法 `success` 按照 `Response` 的职责处理输入，完成 `success` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `success` processes its inputs according to `Response`'s responsibility, performs the `success` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `success` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `success`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        boolean success() {
            return json != null;
        }
    }

    /**
     * 类型 `GatewayAdminSnapshot` 位于 `GatewayAdminControlPlaneStatusClient` 内，是记录类型，用于承载 `Gateway Admin Snapshot` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `GatewayAdminSnapshot` is a record inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Gateway Admin Snapshot`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `GatewayAdminSnapshot` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `GatewayAdminSnapshot` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param release 记录组件 `release` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `release` carries constructor data whose meaning is defined by the record contract.
     * @param providers 记录组件 `providers` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `providers` carries constructor data whose meaning is defined by the record contract.
     * @param consistency 记录组件 `consistency` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `consistency` carries constructor data whose meaning is defined by the record contract.
     * @param checkedAt 记录组件 `checkedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checkedAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record GatewayAdminSnapshot(
            /**
             * 字段 `release` 表示 `GatewayAdminSnapshot` 中与 `release` 相关的状态、依赖、配置或结果（声明类型 `ReleaseObservation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `release` stores the `release`-related state, dependency, configuration, or result of `GatewayAdminSnapshot` (declared type `ReleaseObservation`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `release` 时应保持 `GatewayAdminSnapshot` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `release`, preserve `GatewayAdminSnapshot`'s lifecycle, immutability, and thread-safety constraints.
             */
            ReleaseObservation release,
            /**
             * 字段 `providers` 表示 `GatewayAdminSnapshot` 中与 `providers` 相关的状态、依赖、配置或结果（声明类型 `ProviderObservation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `providers` stores the `providers`-related state, dependency, configuration, or result of `GatewayAdminSnapshot` (declared type `ProviderObservation`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `providers` 时应保持 `GatewayAdminSnapshot` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `providers`, preserve `GatewayAdminSnapshot`'s lifecycle, immutability, and thread-safety constraints.
             */
            ProviderObservation providers,
            /**
             * 字段 `consistency` 表示 `GatewayAdminSnapshot` 中与 `consistency` 相关的状态、依赖、配置或结果（声明类型 `ConsistencyObservation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `consistency` stores the `consistency`-related state, dependency, configuration, or result of `GatewayAdminSnapshot` (declared type `ConsistencyObservation`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `consistency` 时应保持 `GatewayAdminSnapshot` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `consistency`, preserve `GatewayAdminSnapshot`'s lifecycle, immutability, and thread-safety constraints.
             */
            ConsistencyObservation consistency,
            /**
             * 字段 `checkedAt` 表示 `GatewayAdminSnapshot` 中与 `checked At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `checkedAt` stores the `checked At`-related state, dependency, configuration, or result of `GatewayAdminSnapshot` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `checkedAt` 时应保持 `GatewayAdminSnapshot` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `checkedAt`, preserve `GatewayAdminSnapshot`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant checkedAt) {
    }

    /**
     * 类型 `ReleaseObservation` 位于 `GatewayAdminControlPlaneStatusClient` 内，是记录类型，用于承载 `Release Observation` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ReleaseObservation` is a record inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Release Observation`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ReleaseObservation` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ReleaseObservation` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param releaseId 记录组件 `releaseId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `releaseId` carries constructor data whose meaning is defined by the record contract.
     * @param releaseStatus 记录组件 `releaseStatus` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `releaseStatus` carries constructor data whose meaning is defined by the record contract.
     * @param definitionSetId 记录组件 `definitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param publishedVersion 记录组件 `publishedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `publishedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record ReleaseObservation(
            /**
             * 字段 `state` 表示 `ReleaseObservation` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `ReleaseObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `ReleaseObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `ReleaseObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String state,
            /**
             * 字段 `releaseId` 表示 `ReleaseObservation` 中与 `release Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `releaseId` stores the `release Id`-related state, dependency, configuration, or result of `ReleaseObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `releaseId` 时应保持 `ReleaseObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `releaseId`, preserve `ReleaseObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String releaseId,
            /**
             * 字段 `releaseStatus` 表示 `ReleaseObservation` 中与 `release Status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `releaseStatus` stores the `release Status`-related state, dependency, configuration, or result of `ReleaseObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `releaseStatus` 时应保持 `ReleaseObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `releaseStatus`, preserve `ReleaseObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String releaseStatus,
            /**
             * 字段 `definitionSetId` 表示 `ReleaseObservation` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `ReleaseObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `ReleaseObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `ReleaseObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String definitionSetId,
            /**
             * 字段 `publishedVersion` 表示 `ReleaseObservation` 中与 `published Version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `publishedVersion` stores the `published Version`-related state, dependency, configuration, or result of `ReleaseObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `publishedVersion` 时应保持 `ReleaseObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `publishedVersion`, preserve `ReleaseObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String publishedVersion,
            /**
             * 字段 `reasonCode` 表示 `ReleaseObservation` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ReleaseObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ReleaseObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ReleaseObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode) {

        /**
         * 方法 `unknown` 按照 `ReleaseObservation` 的职责处理输入，完成 `unknown` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `unknown` processes its inputs according to `ReleaseObservation`'s responsibility, performs the `unknown` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `unknown` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `unknown`, then continue the business flow using its result, exception, or side effect.
         *
         * @param releaseId 输入参数 `releaseId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        static ReleaseObservation unknown(String releaseId, String reasonCode) {
            return new ReleaseObservation(
                    "UNKNOWN", releaseId, null, null, null, reasonCode);
        }
    }

    /**
     * 类型 `ProviderObservation` 位于 `GatewayAdminControlPlaneStatusClient` 内，是记录类型，用于承载 `Provider Observation` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProviderObservation` is a record inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Provider Observation`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProviderObservation` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProviderObservation` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param instances 记录组件 `instances` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `instances` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record ProviderObservation(
            /**
             * 字段 `state` 表示 `ProviderObservation` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `ProviderObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `ProviderObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `ProviderObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String state,
            /**
             * 字段 `instances` 表示 `ProviderObservation` 中与 `instances` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ProviderInstance&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `instances` stores the `instances`-related state, dependency, configuration, or result of `ProviderObservation` (declared type `List&lt;ProviderInstance&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `instances` 时应保持 `ProviderObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `instances`, preserve `ProviderObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<ProviderInstance> instances,
            /**
             * 字段 `reasonCode` 表示 `ProviderObservation` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ProviderObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ProviderObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ProviderObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode) {

        /**
         * 构造器 `ProviderObservation` 用于创建并初始化 `ProviderObservation` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ProviderObservation` creates and initializes `ProviderObservation`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ProviderObservation` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ProviderObservation`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param state 输入参数 `state`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param instances 输入参数 `instances`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ProviderObservation {
            instances = List.copyOf(instances);
        }

        /**
         * 方法 `unknown` 按照 `ProviderObservation` 的职责处理输入，完成 `unknown` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `unknown` processes its inputs according to `ProviderObservation`'s responsibility, performs the `unknown` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `unknown` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `unknown`, then continue the business flow using its result, exception, or side effect.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        static ProviderObservation unknown(String reasonCode) {
            return new ProviderObservation("UNKNOWN", List.of(), reasonCode);
        }
    }

    /**
     * 类型 `ProviderInstance` 位于 `GatewayAdminControlPlaneStatusClient` 内，是记录类型，用于承载 `Provider Instance` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProviderInstance` is a record inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Provider Instance`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProviderInstance` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProviderInstance` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param instanceId 记录组件 `instanceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `instanceId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param serviceKey 记录组件 `serviceKey` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `serviceKey` carries constructor data whose meaning is defined by the record contract.
     * @param definitionSetId 记录组件 `definitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definitionSetId` carries constructor data whose meaning is defined by the record contract.
     */
    public record ProviderInstance(
            /**
             * 字段 `instanceId` 表示 `ProviderInstance` 中与 `instance Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `instanceId` stores the `instance Id`-related state, dependency, configuration, or result of `ProviderInstance` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `instanceId` 时应保持 `ProviderInstance` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `instanceId`, preserve `ProviderInstance`'s lifecycle, immutability, and thread-safety constraints.
             */
            String instanceId,
            /**
             * 字段 `status` 表示 `ProviderInstance` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `ProviderInstance` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `ProviderInstance` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `ProviderInstance`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `serviceKey` 表示 `ProviderInstance` 中与 `service Key` 相关的状态、依赖、配置或结果（声明类型 `ServiceKey`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `serviceKey` stores the `service Key`-related state, dependency, configuration, or result of `ProviderInstance` (declared type `ServiceKey`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `serviceKey` 时应保持 `ProviderInstance` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `serviceKey`, preserve `ProviderInstance`'s lifecycle, immutability, and thread-safety constraints.
             */
            ServiceKey serviceKey,
            /**
             * 字段 `definitionSetId` 表示 `ProviderInstance` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `ProviderInstance` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `ProviderInstance` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `ProviderInstance`'s lifecycle, immutability, and thread-safety constraints.
             */
            String definitionSetId) {
    }

    /**
     * 类型 `ServiceKey` 位于 `GatewayAdminControlPlaneStatusClient` 内，是记录类型，用于承载 `Service Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ServiceKey` is a record inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Service Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ServiceKey` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ServiceKey` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param bizCode 记录组件 `bizCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `bizCode` carries constructor data whose meaning is defined by the record contract.
     * @param appCode 记录组件 `appCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `appCode` carries constructor data whose meaning is defined by the record contract.
     * @param env 记录组件 `env` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `env` carries constructor data whose meaning is defined by the record contract.
     * @param namespace 记录组件 `namespace` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `namespace` carries constructor data whose meaning is defined by the record contract.
     * @param serviceKind 记录组件 `serviceKind` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `serviceKind` carries constructor data whose meaning is defined by the record contract.
     * @param protocol 记录组件 `protocol` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `protocol` carries constructor data whose meaning is defined by the record contract.
     * @param serviceName 记录组件 `serviceName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `serviceName` carries constructor data whose meaning is defined by the record contract.
     * @param group 记录组件 `group` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `group` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record ServiceKey(
            /**
             * 字段 `bizCode` 表示 `ServiceKey` 中与 `biz Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `bizCode` stores the `biz Code`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `bizCode` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `bizCode`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String bizCode,
            /**
             * 字段 `appCode` 表示 `ServiceKey` 中与 `app Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `appCode` stores the `app Code`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `appCode` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `appCode`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String appCode,
            /**
             * 字段 `env` 表示 `ServiceKey` 中与 `env` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `env` stores the `env`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `env` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `env`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String env,
            /**
             * 字段 `namespace` 表示 `ServiceKey` 中与 `namespace` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `namespace` stores the `namespace`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `namespace` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `namespace`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String namespace,
            /**
             * 字段 `serviceKind` 表示 `ServiceKey` 中与 `service Kind` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `serviceKind` stores the `service Kind`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `serviceKind` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `serviceKind`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String serviceKind,
            /**
             * 字段 `protocol` 表示 `ServiceKey` 中与 `protocol` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `protocol` stores the `protocol`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `protocol` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `protocol`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String protocol,
            /**
             * 字段 `serviceName` 表示 `ServiceKey` 中与 `service Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `serviceName` stores the `service Name`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `serviceName` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `serviceName`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String serviceName,
            /**
             * 字段 `group` 表示 `ServiceKey` 中与 `group` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `group` stores the `group`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `group` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `group`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String group,
            /**
             * 字段 `version` 表示 `ServiceKey` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `ServiceKey` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `ServiceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `ServiceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            String version) {

        /**
         * 方法 `validated` 按照 `ServiceKey` 的职责处理输入，完成 `validated` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `validated` processes its inputs according to `ServiceKey`'s responsibility, performs the `validated` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `validated` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `validated`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        ServiceKey validated() {
            return new ServiceKey(
                    required(bizCode, "serviceKey.bizCode"),
                    required(appCode, "serviceKey.appCode"),
                    required(env, "serviceKey.env"),
                    required(namespace, "serviceKey.namespace"),
                    required(serviceKind, "serviceKey.serviceKind"),
                    required(protocol, "serviceKey.protocol"),
                    required(serviceName, "serviceKey.serviceName"),
                    required(group, "serviceKey.group"),
                    required(version, "serviceKey.version"));
        }
    }

    /**
     * 类型 `ConsistencyObservation` 位于 `GatewayAdminControlPlaneStatusClient` 内，是记录类型，用于承载 `Consistency Observation` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConsistencyObservation` is a record inside `GatewayAdminControlPlaneStatusClient` and carries the responsibility, state, or contract for `Consistency Observation`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConsistencyObservation` 作为 `GatewayAdminControlPlaneStatusClient` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConsistencyObservation` as the responsibility boundary of `GatewayAdminControlPlaneStatusClient`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     * @param releaseId 记录组件 `releaseId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `releaseId` carries constructor data whose meaning is defined by the record contract.
     * @param releaseStatus 记录组件 `releaseStatus` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `releaseStatus` carries constructor data whose meaning is defined by the record contract.
     * @param consistent 记录组件 `consistent` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `consistent` carries constructor data whose meaning is defined by the record contract.
     * @param observedVersion 记录组件 `observedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `observedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record ConsistencyObservation(
            /**
             * 字段 `state` 表示 `ConsistencyObservation` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `state` stores the `state`-related state, dependency, configuration, or result of `ConsistencyObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `state` 时应保持 `ConsistencyObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `state`, preserve `ConsistencyObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String state,
            /**
             * 字段 `releaseId` 表示 `ConsistencyObservation` 中与 `release Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `releaseId` stores the `release Id`-related state, dependency, configuration, or result of `ConsistencyObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `releaseId` 时应保持 `ConsistencyObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `releaseId`, preserve `ConsistencyObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String releaseId,
            /**
             * 字段 `releaseStatus` 表示 `ConsistencyObservation` 中与 `release Status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `releaseStatus` stores the `release Status`-related state, dependency, configuration, or result of `ConsistencyObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `releaseStatus` 时应保持 `ConsistencyObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `releaseStatus`, preserve `ConsistencyObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String releaseStatus,
            /**
             * 字段 `consistent` 表示 `ConsistencyObservation` 中与 `consistent` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `consistent` stores the `consistent`-related state, dependency, configuration, or result of `ConsistencyObservation` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `consistent` 时应保持 `ConsistencyObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `consistent`, preserve `ConsistencyObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean consistent,
            /**
             * 字段 `observedVersion` 表示 `ConsistencyObservation` 中与 `observed Version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `observedVersion` stores the `observed Version`-related state, dependency, configuration, or result of `ConsistencyObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `observedVersion` 时应保持 `ConsistencyObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `observedVersion`, preserve `ConsistencyObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String observedVersion,
            /**
             * 字段 `reasonCode` 表示 `ConsistencyObservation` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ConsistencyObservation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ConsistencyObservation` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ConsistencyObservation`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode) {

        /**
         * 方法 `unknown` 按照 `ConsistencyObservation` 的职责处理输入，完成 `unknown` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `unknown` processes its inputs according to `ConsistencyObservation`'s responsibility, performs the `unknown` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `unknown` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `unknown`, then continue the business flow using its result, exception, or side effect.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        static ConsistencyObservation unknown(String reasonCode) {
            return new ConsistencyObservation(
                    "UNKNOWN", null, null, false, null, reasonCode);
        }
    }
}
