package top.egon.cola.platform.rbac3.admin.runtime.repository.http;

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
import top.egon.cola.platform.rbac3.admin.runtime.repository.http.GatewayAdminControlPlaneTransport;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayAdminControlPlaneHttpResponseVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.internal.JdkGatewayAdminControlPlaneTransport;
import top.egon.cola.platform.rbac3.admin.runtime.repository.internal.GatewayAdminControlPlaneResponse;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayAdminSnapshotVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayReleaseObservationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayProviderObservationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayProviderInstanceVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.GatewayServiceKey;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayConsistencyObservationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.BearerCredentialVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.GatewayAdminSnapshotRepository;

/**
 * 类型 `GatewayAdminControlPlaneStatusClient` 位于当前包内，是类型，用于承载 `Gateway Admin Control Plane Status Client` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `GatewayAdminControlPlaneStatusClient` is a type in its package and carries the responsibility, state, or contract for `Gateway Admin Control Plane Status Client`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Read-only typed client for Gateway Admin release and discovery observations.
 */
public final class GatewayAdminControlPlaneStatusClient
        implements GatewayAdminSnapshotRepository {

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
     * 字段 `providerServiceKey` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `provider Service Key` 相关的状态、依赖、配置或结果（声明类型 `GatewayServiceKey`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `providerServiceKey` stores the `provider Service Key`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `GatewayServiceKey`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `providerServiceKey` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `providerServiceKey`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final GatewayServiceKey providerServiceKey;
    /**
     * 字段 `credentials` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `credentials` 相关的状态、依赖、配置或结果（声明类型 `GatewayAdminStatusCredentialProvider`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentials` stores the `credentials`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `GatewayAdminStatusCredentialProvider`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentials` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentials`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final GatewayAdminStatusCredentialProvider credentials;
    /**
     * 字段 `transport` 表示 `GatewayAdminControlPlaneStatusClient` 中与 `transport` 相关的状态、依赖、配置或结果（声明类型 `GatewayAdminControlPlaneTransport`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `transport` stores the `transport`-related state, dependency, configuration, or result of `GatewayAdminControlPlaneStatusClient` (declared type `GatewayAdminControlPlaneTransport`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `transport` 时应保持 `GatewayAdminControlPlaneStatusClient` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `transport`, preserve `GatewayAdminControlPlaneStatusClient`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final GatewayAdminControlPlaneTransport transport;
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
        this(adminBaseUri, gatewayGroupId, releaseId, new GatewayServiceKey(
                        "rbac3", "rbac3-admin", "prod", "default",
                        "HTTP_PROVIDER", "http",
                        "rbac3-admin", "default", "1.0.0"),
                credentials, new JdkGatewayAdminControlPlaneTransport(timeout), objectMapper, clock, timeout);
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
            GatewayServiceKey providerServiceKey,
            GatewayAdminStatusCredentialProvider credentials,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this(adminBaseUri, gatewayGroupId, releaseId, providerServiceKey,
                credentials, new JdkGatewayAdminControlPlaneTransport(timeout), objectMapper, clock, timeout);
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
            GatewayAdminControlPlaneTransport transport,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this(adminBaseUri, gatewayGroupId, releaseId, new GatewayServiceKey(
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
            GatewayServiceKey providerServiceKey,
            GatewayAdminStatusCredentialProvider credentials,
            GatewayAdminControlPlaneTransport transport,
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
    @Override
    public GatewayAdminSnapshotVO snapshot() {
        Instant checkedAt = clock.instant();
        Optional<BearerCredentialVO> supplied =
                credentials.current();
        if (supplied.isEmpty()) {
            return unknown("CREDENTIAL_MISSING", checkedAt);
        }
        var credential = supplied.orElseThrow();
        if (!credential.expiresAt().isAfter(checkedAt)) {
            return unknown("CREDENTIAL_EXPIRED", checkedAt);
        }
        return new GatewayAdminSnapshotVO(
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
    private GatewayReleaseObservationVO release(String token) {
        GatewayAdminControlPlaneResponse response = get("/api/v1/gateway/admin/releases/" + encode(releaseId), token);
        if (!response.success()) {
            return GatewayReleaseObservationVO.unknown(releaseId, response.reasonCode());
        }
        JsonNode json = response.json();
        return new GatewayReleaseObservationVO(
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
    private GatewayProviderObservationVO providers(String token) {
        String query = "?bizCode=" + encode(providerServiceKey.bizCode())
                + "&appCode=" + encode(providerServiceKey.appCode())
                + "&env=" + encode(providerServiceKey.env())
                + "&namespace=" + encode(providerServiceKey.namespace())
                + "&serviceKind=" + encode(providerServiceKey.serviceKind())
                + "&protocol=" + encode(providerServiceKey.protocol())
                + "&serviceName=" + encode(providerServiceKey.serviceName())
                + "&group=" + encode(providerServiceKey.group())
                + "&version=" + encode(providerServiceKey.version());
        GatewayAdminControlPlaneResponse response = get(
                "/api/v1/gateway/admin/providers/instances" + query, token);
        if (!response.success()) {
            return GatewayProviderObservationVO.unknown(response.reasonCode());
        }
        List<GatewayProviderInstanceVO> instances = new ArrayList<>();
        collectInstances(response.json(), instances);
        return new GatewayProviderObservationVO("SUCCESS", instances, null);
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
    private GatewayConsistencyObservationVO consistency(String token) {
        GatewayAdminControlPlaneResponse response = get(
                "/api/v1/gateway/admin/gateway-groups/" + encode(gatewayGroupId)
                        + "/runtime-consistency", token);
        if (!response.success()) {
            return GatewayConsistencyObservationVO.unknown(response.reasonCode());
        }
        JsonNode json = response.json();
        return new GatewayConsistencyObservationVO(
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
    private GatewayAdminControlPlaneResponse get(String path, String token) {
        try {
            GatewayAdminControlPlaneHttpResponseVO result = transport.get(adminBaseUri.resolve(path), token, timeout);
            if (result.statusCode() == 403) {
                return GatewayAdminControlPlaneResponse.failure("GATEWAY_STATUS_FORBIDDEN");
            }
            if (result.statusCode() < 200 || result.statusCode() >= 300) {
                return GatewayAdminControlPlaneResponse.failure("GATEWAY_STATUS_UNAVAILABLE");
            }
            return GatewayAdminControlPlaneResponse.success(objectMapper.readTree(result.body()));
        } catch (Exception unavailable) {
            return GatewayAdminControlPlaneResponse.failure("GATEWAY_STATUS_UNAVAILABLE");
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
    private void collectInstances(JsonNode node, List<GatewayProviderInstanceVO> target) {
        if (node.isArray()) {
            node.forEach(value -> collectInstances(value, target));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode serviceKey = node.get("serviceKey");
        if (serviceKey != null && serviceKey.isObject() && node.has("instanceId")) {
            target.add(new GatewayProviderInstanceVO(
                    text(node, "instanceId"), text(node, "status"),
                    new GatewayServiceKey(
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
    private GatewayAdminSnapshotVO unknown(String reasonCode, Instant checkedAt) {
        return new GatewayAdminSnapshotVO(
                GatewayReleaseObservationVO.unknown(releaseId, reasonCode),
                GatewayProviderObservationVO.unknown(reasonCode),
                GatewayConsistencyObservationVO.unknown(reasonCode),
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










    }
