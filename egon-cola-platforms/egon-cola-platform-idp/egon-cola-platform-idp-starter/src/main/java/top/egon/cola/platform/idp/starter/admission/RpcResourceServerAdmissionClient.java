package top.egon.cola.platform.idp.starter.admission;

import com.google.protobuf.Timestamp;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionRequest;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientHandle;
import top.egon.cola.platform.idp.rpc.contract.ResourceServerAdmissionRpc;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionResponse;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 通过静态目标直连 IdP Egon-RPC，取得并解析短期 Resource Server 准入票据。
 * 该客户端不经过 Gateway 或 DDC 服务发现，因而不会在取得 DDC Admission Ticket 前形成
 * 启动依赖环。
 *
 * <p>Obtains and parses short-lived Resource Server admission tickets by connecting directly to
 * a statically configured IdP Egon-RPC target. The client bypasses Gateway and DDC discovery, so
 * it does not introduce a startup dependency cycle before a DDC Admission Ticket is available.</p>
 *
 * <p>客户端把 RPC 响应解析为本地调度模型并严格核对业务身份；Admission JWT 的权威签名
 * 验证仍由 DDC Registry 执行。</p>
 *
 * <p>The client parses the RPC response into a local scheduling model and strictly matches its
 * business identity. Authoritative Admission JWT signature verification remains the
 * responsibility of the DDC Registry.</p>
 */
public final class RpcResourceServerAdmissionClient implements AutoCloseable {

    /** RFC 7523 Client Assertion 类型；RFC 7523 Client Assertion type. */
    private static final String ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /** Admission JWT 类型；Admission JWT type. */
    private static final JOSEObjectType ADMISSION_TYPE =
            new JOSEObjectType("rs-admission+jwt");

    /** Admission JWT 用途；Admission JWT use. */
    private static final String ADMISSION_USE = "resource_server_admission";

    /** Admission JWT Audience；Admission JWT audience. */
    private static final List<String> ADMISSION_AUDIENCE =
            List.of("ddc-registry");

    /** 类型化 IdP RPC 客户端；typed IdP RPC client. */
    private final ResourceServerAdmissionRpc rpc;

    /** 专属 RPC Channel 关闭动作；dedicated RPC channel close action. */
    private final Runnable closeAction;

    /** 受信任 IdP Issuer；trusted IdP issuer. */
    private final String issuer;

    /** Client Assertion 工厂；Client Assertion factory. */
    private final PrivateKeyJwtAssertionFactory assertions;

    /**
     * 使用拥有专属 Channel 的直连句柄创建生产客户端。
     *
     * <p>Creates the production client from a direct handle owning its dedicated channel.</p>
     *
     * @param handle IdP RPC 直连句柄；direct IdP RPC handle
     * @param issuer 受信任 IdP Issuer；trusted IdP issuer
     * @param assertions RPC Audience 绑定的 Client Assertion 工厂；RPC audience-bound Client
     *                   Assertion factory
     */
    public RpcResourceServerAdmissionClient(
            RpcDirectClientHandle<ResourceServerAdmissionRpc> handle,
            String issuer,
            PrivateKeyJwtAssertionFactory assertions
    ) {
        this(
                Objects.requireNonNull(handle, "handle").client(),
                handle::close,
                issuer,
                assertions
        );
    }

    /**
     * 使用可控 RPC 契约创建同包测试客户端。
     *
     * <p>Creates a package-level test client from a controllable RPC contract.</p>
     *
     * @param rpc 可控 RPC 契约；controllable RPC contract
     * @param issuer 受信任 IdP Issuer；trusted IdP issuer
     * @param assertions Client Assertion 工厂；Client Assertion factory
     */
    RpcResourceServerAdmissionClient(
            ResourceServerAdmissionRpc rpc,
            String issuer,
            PrivateKeyJwtAssertionFactory assertions
    ) {
        this(rpc, () -> { }, issuer, assertions);
    }

    /**
     * 使用显式 RPC、生命周期动作和安全上下文创建客户端。
     *
     * <p>Creates a client with explicit RPC, lifecycle action, and security context.</p>
     */
    private RpcResourceServerAdmissionClient(
            ResourceServerAdmissionRpc rpc,
            Runnable closeAction,
            String issuer,
            PrivateKeyJwtAssertionFactory assertions
    ) {
        this.rpc = Objects.requireNonNull(rpc, "rpc");
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
        this.issuer = normalizedIssuer(issuer);
        this.assertions = Objects.requireNonNull(assertions, "assertions");
    }

    /**
     * 为一个精确 DDC 实例申请新的 Admission Ticket。
     *
     * <p>Requests a new Admission Ticket for one exact DDC instance.</p>
     *
     * @param request 精确 DDC 准入请求；exact DDC admission request
     * @return 已解析且与请求匹配的票据；parsed ticket matching the request
     * @throws IllegalStateException IdP RPC 不可用或响应不可信时 Fail Closed；fails closed
     *                               when IdP RPC is unavailable or its response is untrusted
     */
    public DdcAdmissionTicket request(DdcAdmissionRequest request) {
        Objects.requireNonNull(request, "request");
        IssueResourceServerAdmissionResponse response;
        try {
            response = rpc.issueAdmission(rpcRequest(request));
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
        if (response == null) {
            throw invalidResponse();
        }
        return ticket(response, request);
    }

    /**
     * 关闭本客户端拥有的专属 RPC Channel。
     *
     * <p>Closes the dedicated RPC channel owned by this client.</p>
     */
    @Override
    public void close() {
        closeAction.run();
    }

    /**
     * 构造不包含私钥材料的 Protobuf 请求。
     *
     * <p>Builds the Protobuf request without private-key material.</p>
     *
     * @param request DDC 准入请求；DDC admission request
     * @return IdP RPC 请求；IdP RPC request
     */
    private IssueResourceServerAdmissionRequest rpcRequest(
            DdcAdmissionRequest request
    ) {
        return IssueResourceServerAdmissionRequest.newBuilder()
                .setClientAssertionType(ASSERTION_TYPE)
                .setClientId(assertions.clientId())
                .setClientAssertion(assertions.create())
                .setResourceServerId(request.resourceServerId())
                .setResource(request.resourceUri().toString())
                .setBiz(request.bizCode())
                .setApp(request.appCode())
                .setEnv(request.environment())
                .setInstanceId(request.instanceId())
                .build();
    }

    /**
     * 解析独立用途 JWT，并校验响应身份与调度时间。
     *
     * <p>Parses the independently scoped JWT and validates response identity and scheduling
     * time.</p>
     *
     * @param response Admission RPC 响应；Admission RPC response
     * @param request 原始 DDC 请求；original DDC request
     * @return DDC 中立票据模型；DDC-neutral ticket model
     */
    private DdcAdmissionTicket ticket(
            IssueResourceServerAdmissionResponse response,
            DdcAdmissionRequest request
    ) {
        try {
            SignedJWT jwt = SignedJWT.parse(required(
                    response.getTicket(),
                    "ticket"
            ));
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                    || !ADMISSION_TYPE.equals(jwt.getHeader().getType())
                    || !response.hasExpiresAt()) {
                throw invalidResponse();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant expiresAt = Objects.requireNonNull(
                    claims.getExpirationTime(),
                    "exp"
            ).toInstant();
            if (!issuer.equals(claims.getIssuer())
                    || !request.resourceServerId().equals(claims.getSubject())
                    || !ADMISSION_AUDIENCE.equals(claims.getAudience())
                    || !ADMISSION_USE.equals(claims.getStringClaim("token_use"))
                    || !expiresAt.equals(instant(response.getExpiresAt()))) {
                throw invalidResponse();
            }
            DdcAdmissionTicket ticket = new DdcAdmissionTicket(
                    response.getTicket(),
                    expiresAt,
                    claims.getSubject(),
                    URI.create(claims.getStringClaim("resource")),
                    number(claims, "resource_version"),
                    claims.getStringClaim("biz"),
                    claims.getStringClaim("app"),
                    claims.getStringClaim("env"),
                    claims.getStringClaim("instance_id"),
                    claims.getStringClaim("credential_id")
            );
            if (!ticket.matches(request)
                    || !assertions.keyId().equals(ticket.credentialId())) {
                throw invalidResponse();
            }
            return ticket;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidResponse();
        }
    }

    /**
     * 将 Protobuf Timestamp 转换为严格 Instant。
     *
     * <p>Converts a Protobuf Timestamp to a strict Instant.</p>
     *
     * @param value Protobuf 时间戳；Protobuf timestamp
     * @return 时间点；instant
     */
    private static Instant instant(Timestamp value) {
        if (value.getNanos() < 0 || value.getNanos() > 999_999_999) {
            throw invalidResponse();
        }
        try {
            return Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    /**
     * 读取非负整数声明。
     *
     * <p>Reads a non-negative integer claim.</p>
     *
     * @param claims JWT 声明；JWT claims
     * @param name 声明名；claim name
     * @return long 值；long value
     */
    private static long number(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        if (!(value instanceof Number number) || number.longValue() < 0) {
            throw invalidResponse();
        }
        return number.longValue();
    }

    /** 创建统一的不可信响应异常。 / Creates the uniform untrusted-response exception. */
    private static IllegalStateException invalidResponse() {
        return new IllegalStateException("IDP_RESOURCE_ADMISSION_UNAVAILABLE");
    }

    /**
     * 创建保留根因的 RPC 不可用异常。
     *
     * <p>Creates an RPC-unavailable exception retaining its root cause.</p>
     *
     * @param cause RPC 或 Assertion 创建失败；RPC or assertion-creation failure
     * @return RPC 不可用异常；RPC-unavailable exception
     */
    private static IllegalStateException unavailable(RuntimeException cause) {
        return new IllegalStateException(
                "IDP_RESOURCE_ADMISSION_UNAVAILABLE",
                cause
        );
    }

    /**
     * 规范化并校验 IdP Issuer。
     *
     * <p>Normalizes and validates the IdP issuer.</p>
     *
     * @param value Issuer 文本；issuer text
     * @return 无末尾斜杠的 Issuer；issuer without a trailing slash
     */
    private static String normalizedIssuer(String value) {
        String issuer = required(value, "issuer");
        URI uri = URI.create(issuer);
        if (!uri.isAbsolute()
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("issuer is invalid");
        }
        return issuer.endsWith("/")
                ? issuer.substring(0, issuer.length() - 1)
                : issuer;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验文本；validated text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw invalidResponse();
        }
        return value;
    }
}
