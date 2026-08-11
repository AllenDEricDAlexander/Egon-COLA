package top.egon.cola.platform.idp.starter.admission;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionRequest;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 通过 IdP Admission Endpoint 取得并解析短期 Resource Server 准入票据。
 *
 * <p>Obtains and parses short-lived Resource Server admission tickets through the IdP Admission
 * Endpoint.</p>
 *
 * <p>客户端只把 HTTPS 响应解析为本地调度模型；Admission JWT 的权威签名验证由 DDC
 * Registry 执行。</p>
 *
 * <p>This client parses the HTTPS response only into a local scheduling model. Authoritative
 * Admission JWT signature verification is performed by the DDC Registry.</p>
 */
public final class HttpResourceServerAdmissionClient {

    /** RFC 7523 Client Assertion 类型；RFC 7523 Client Assertion type. */
    private static final String ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /** Admission JWT 类型；Admission JWT type. */
    private static final JOSEObjectType ADMISSION_TYPE =
            new JOSEObjectType("rs-admission+jwt");

    /** Admission JWT 用途；Admission JWT use. */
    private static final String ADMISSION_USE =
            "resource_server_admission";

    /** Admission JWT Audience；Admission JWT audience. */
    private static final List<String> ADMISSION_AUDIENCE =
            List.of("ddc-registry");

    /** HTTP 客户端；HTTP client. */
    private final RestClient restClient;

    /** Admission Endpoint URI；Admission Endpoint URI. */
    private final URI endpoint;

    /** 受信任 IdP Issuer；trusted IdP issuer. */
    private final String issuer;

    /** Client Assertion 工厂；Client Assertion factory. */
    private final PrivateKeyJwtAssertionFactory assertions;

    /**
     * 创建 IdP Resource Server Admission HTTP 客户端。
     *
     * <p>Creates the IdP Resource Server Admission HTTP client.</p>
     *
     * @param restClient Spring HTTP 客户端；Spring HTTP client
     * @param endpoint Admission Endpoint URI；Admission Endpoint URI
     * @param issuer 受信任 IdP Issuer；trusted IdP issuer
     * @param assertions 端点绑定 Client Assertion 工厂；endpoint-bound Client Assertion factory
     */
    public HttpResourceServerAdmissionClient(
            RestClient restClient,
            URI endpoint,
            String issuer,
            PrivateKeyJwtAssertionFactory assertions
    ) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.endpoint = endpoint(endpoint);
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
     * @throws IllegalStateException IdP 不可用或响应不可信时 Fail Closed；fails closed when IdP
     * is unavailable or its response is untrusted
     */
    public DdcAdmissionTicket request(DdcAdmissionRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            AdmissionResponse response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form(request))
                    .retrieve()
                    .body(AdmissionResponse.class);
            if (response == null) {
                throw invalidResponse();
            }
            return ticket(response, request);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "IDP_RESOURCE_ADMISSION_UNAVAILABLE",
                    exception
            );
        }
    }

    /**
     * 构造不包含私钥的端点表单。
     *
     * <p>Builds the endpoint form without private-key material.</p>
     *
     * @param request DDC 准入请求；DDC admission request
     * @return 单值表单；single-valued form
     */
    private MultiValueMap<String, String> form(DdcAdmissionRequest request) {
        LinkedMultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();
        form.add("client_id", assertions.clientId());
        form.add("client_assertion_type", ASSERTION_TYPE);
        form.add("client_assertion", assertions.create());
        form.add("resource_server_id", request.resourceServerId());
        form.add("resource", request.resourceUri().toString());
        form.add("biz", request.bizCode());
        form.add("app", request.appCode());
        form.add("env", request.environment());
        form.add("instance_id", request.instanceId());
        return form;
    }

    /**
     * 解析独立用途 JWT，并校验响应身份与调度时间。
     *
     * <p>Parses the independently scoped JWT and validates response identity and scheduling
     * time.</p>
     *
     * @param response Admission HTTP 响应；Admission HTTP response
     * @param request 原始 DDC 请求；original DDC request
     * @return DDC 中立票据模型；DDC-neutral ticket model
     */
    private DdcAdmissionTicket ticket(
            AdmissionResponse response,
            DdcAdmissionRequest request
    ) {
        try {
            SignedJWT jwt = SignedJWT.parse(required(
                    response.ticket(),
                    "ticket"
            ));
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                    || !ADMISSION_TYPE.equals(jwt.getHeader().getType())) {
                throw invalidResponse();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant expiresAt = Objects.requireNonNull(
                    claims.getExpirationTime(),
                    "exp"
            ).toInstant();
            if (!issuer.equals(claims.getIssuer())
                    || !request.resourceServerId().equals(
                            claims.getSubject())
                    || !ADMISSION_AUDIENCE.equals(claims.getAudience())
                    || !ADMISSION_USE.equals(claims.getStringClaim(
                            "token_use"))
                    || response.expiresAt() == null
                    || response.expiresAt().getEpochSecond()
                    != expiresAt.getEpochSecond()) {
                throw invalidResponse();
            }
            DdcAdmissionTicket ticket = new DdcAdmissionTicket(
                    response.ticket(),
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
     * 读取非负整数声明。
     *
     * <p>Reads a non-negative integer claim.</p>
     *
     * @param claims JWT 声明；JWT claims
     * @param name 声明名；claim name
     * @return long 值；long value
     */
    private static long number(JWTClaimsSet claims, String name)
            throws java.text.ParseException {
        Object value = claims.getClaim(name);
        if (!(value instanceof Number number)) {
            throw invalidResponse();
        }
        return number.longValue();
    }

    /**
     * 校验 Admission Endpoint URI。
     *
     * <p>Validates the Admission Endpoint URI.</p>
     *
     * @param value Endpoint URI；Endpoint URI
     * @return 已校验 URI；validated URI
     */
    private static URI endpoint(URI value) {
        Objects.requireNonNull(value, "endpoint");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || value.getQuery() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException("endpoint is invalid");
        }
        return value;
    }

    /**
     * 创建统一的不可信响应异常。
     *
     * <p>Creates the uniform untrusted-response exception.</p>
     *
     * @return 不可信响应异常；untrusted-response exception
     */
    private static IllegalStateException invalidResponse() {
        return new IllegalStateException(
                "IDP_RESOURCE_ADMISSION_UNAVAILABLE"
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

    /**
     * IdP Admission Endpoint 的最小 JSON 响应。
     *
     * <p>Minimal JSON response from the IdP Admission Endpoint.</p>
     *
     * @param ticket 紧凑 Admission JWT；compact Admission JWT
     * @param expiresAt 本地续签调度时间；local renewal scheduling time
     */
    private record AdmissionResponse(String ticket, Instant expiresAt) {

        /**
         * 校验最小响应且不保留空票据。
         *
         * <p>Validates the minimal response and rejects an empty ticket.</p>
         */
        private AdmissionResponse {
            ticket = required(ticket, "ticket");
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        }

        /**
         * 返回不包含原始 JWT 的安全诊断文本。
         *
         * <p>Returns safe diagnostic text excluding the raw JWT.</p>
         *
         * @return 脱敏响应；redacted response
         */
        @Override
        public String toString() {
            return "AdmissionResponse["
                    + "ticket=<redacted>, expiresAt=" + expiresAt + ']';
        }
    }
}
