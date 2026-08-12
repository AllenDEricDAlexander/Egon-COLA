package top.egon.cola.component.gateway.admin.interfaces.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialStore;
import top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayHmacNonceStore;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayReportAuthentication;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.contract.reporting.GatewayCanonicalRequest;
import top.egon.cola.component.gateway.contract.reporting.GatewayRequestSigner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayReportHmacFilter} 是过滤器，位于当前 Gateway 模块的相关包中，负责网关报告Hmac过滤器相关的职责与边界。
 * English summary: {@code GatewayReportHmacFilter} is a gateway report hmac filter filter in the current Gateway module; it owns the gateway report hmac filter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Component
public class GatewayReportHmacFilter extends OncePerRequestFilter {

    /**
     * 中文说明：表示 PREFIX 这一固定值；它属于 {@code GatewayReportHmacFilter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value prefix; it is a state, type, or protocol value of {@code GatewayReportHmacFilter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String PREFIX =
            "/api/v1/gateway/openapi/interface-definitions/";

    /**
     * 中文说明：表示 MAXBODYBYTES 这一固定值；它属于 {@code GatewayReportHmacFilter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max body bytes; it is a state, type, or protocol value of {@code GatewayReportHmacFilter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_BODY_BYTES = 8 * 1024 * 1024;

    /**
     * 中文说明：保存 credentials 对应的状态、依赖或配置值；字段类型为 {@code GatewayCredentialStore}，由 {@code GatewayReportHmacFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by credentials; its type is {@code GatewayCredentialStore}, and {@code GatewayReportHmacFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCredentialStore credentials;

    /**
     * 中文说明：保存 applications 对应的状态、依赖或配置值；字段类型为 {@code GatewayApplicationRepository}，由 {@code GatewayReportHmacFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by applications; its type is {@code GatewayApplicationRepository}, and {@code GatewayReportHmacFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayApplicationRepository applications;

    /**
     * 中文说明：保存 nonces 对应的状态、依赖或配置值；字段类型为 {@code GatewayHmacNonceStore}，由 {@code GatewayReportHmacFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by nonces; its type is {@code GatewayHmacNonceStore}, and {@code GatewayReportHmacFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHmacNonceStore nonces;

    /**
     * 中文说明：保存 protector 对应的状态、依赖或配置值；字段类型为 {@code GatewaySecretProtector}，由 {@code GatewayReportHmacFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by protector; its type is {@code GatewaySecretProtector}, and {@code GatewayReportHmacFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewaySecretProtector protector;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code GatewayReportHmacFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code GatewayReportHmacFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 allowedSkew 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayReportHmacFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by allowed skew; its type is {@code Duration}, and {@code GatewayReportHmacFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration allowedSkew;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayReportHmacFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayReportHmacFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 signer 对应的状态、依赖或配置值；字段类型为 {@code GatewayRequestSigner}，由 {@code GatewayReportHmacFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by signer; its type is {@code GatewayRequestSigner}, and {@code GatewayReportHmacFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRequestSigner signer = new GatewayRequestSigner();

    /**
     * 中文说明：创建 {@code GatewayReportHmacFilter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReportHmacFilter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param credentials 参数 credentials；parameter credentials。
     * @param applications 参数 applications；parameter applications。
     * @param nonces 参数 nonces；parameter nonces。
     * @param protector 参数 protector；parameter protector。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param allowedSkew 参数 allowedSkew；parameter allowed skew。
     */
    @Autowired
    public GatewayReportHmacFilter(
            GatewayCredentialStore credentials,
            GatewayApplicationRepository applications,
            GatewayHmacNonceStore nonces,
            ObjectProvider<GatewaySecretProtector> protector,
            ObjectMapper objectMapper,
            @Value("${gateway.admin.hmac.allowed-skew:PT5M}")
            Duration allowedSkew) {
        this(
                credentials,
                applications,
                nonces,
                protector.getIfAvailable(),
                objectMapper,
                allowedSkew,
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayReportHmacFilter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReportHmacFilter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param credentials 参数 credentials；parameter credentials。
     * @param applications 参数 applications；parameter applications。
     * @param nonces 参数 nonces；parameter nonces。
     * @param protector 参数 protector；parameter protector。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param allowedSkew 参数 allowedSkew；parameter allowed skew。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayReportHmacFilter(
            GatewayCredentialStore credentials,
            GatewayApplicationRepository applications,
            GatewayHmacNonceStore nonces,
            GatewaySecretProtector protector,
            ObjectMapper objectMapper,
            Duration allowedSkew,
            Clock clock) {
        this.credentials = credentials;
        this.applications = applications;
        this.nonces = nonces;
        this.protector = protector;
        this.objectMapper = objectMapper;
        this.allowedSkew = allowedSkew;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 shouldNot过滤器 操作；该方法是 {@code GatewayReportHmacFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the should not filter operation; this method is the invocation entry point on {@code GatewayReportHmacFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.shouldNotFilter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 shouldNot过滤器 的处理结果；returns the result of the operation.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIX);
    }

    /**
     * 中文说明：执行 do过滤器Internal 操作；该方法是 {@code GatewayReportHmacFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the do filter internal operation; this method is the invocation entry point on {@code GatewayReportHmacFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.doFilterInternal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param response 参数 响应；parameter response。
     * @param chain 参数 chain；parameter chain。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            CachedBodyRequest cached = new CachedBodyRequest(request);
            if (cached.body.length > MAX_BODY_BYTES) {
                throw new AuthenticationFailure(
                        413,
                        "GATEWAY_REPORT_BODY_TOO_LARGE"
                );
            }
            GatewayReportAuthentication authentication =
                    authenticate(cached);
            cached.setAttribute(
                    GatewayReportAuthentication.REQUEST_ATTRIBUTE,
                    authentication
            );
            chain.doFilter(cached, response);
        } catch (AuthenticationFailure failure) {
            response.setStatus(failure.status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    response.getOutputStream(),
                    Map.of(
                            "code", failure.code,
                            "message", failure.getMessage(),
                            "timestamp", clock.instant()
                    )
            );
        }
    }

    /**
     * 中文说明：执行 authenticate 操作；该方法是 {@code GatewayReportHmacFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authenticate operation; this method is the invocation entry point on {@code GatewayReportHmacFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.authenticate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 authenticate 的处理结果；returns the result of the operation.
     */
    private GatewayReportAuthentication authenticate(
            CachedBodyRequest request) {
        if (request.getHeader("X-Admin-Actor-Id") != null) {
            throw failure("GATEWAY_REPORT_CREDENTIAL_REQUIRED");
        }
        String accessKey = header(
                request,
                GatewayRequestSigner.ACCESS_KEY_HEADER
        );
        long timestamp = timestamp(request);
        Instant now = clock.instant();
        Instant signedAt = Instant.ofEpochMilli(timestamp);
        if (Duration.between(signedAt, now).abs().compareTo(allowedSkew) > 0) {
            throw failure("GATEWAY_REPORT_TIMESTAMP_INVALID");
        }
        GatewayCredentialStore.CredentialRecord credential =
                credentials.findByAccessKey(accessKey)
                        .filter(value -> active(value, now))
                        .orElseThrow(() ->
                                failure("GATEWAY_REPORT_CREDENTIAL_INVALID"));
        GatewayApplicationEntity application =
                applications.findByIdAndDeletedFalse(
                                credential.applicationId()
                        )
                        .orElseThrow(() ->
                                failure("GATEWAY_REPORT_SCOPE_INVALID"));
        String requestedApplication = header(
                request,
                "X-Gateway-Application-Code"
        );
        if (!application.getApplicationCode().equals(
                requestedApplication
        )) {
            throw failure("GATEWAY_REPORT_SCOPE_INVALID");
        }
        if (protector == null) {
            throw new AuthenticationFailure(
                    503,
                    "GATEWAY_REPORT_SECRET_PROTECTOR_UNAVAILABLE"
            );
        }
        String nonce = header(request, GatewayRequestSigner.NONCE_HEADER);
        GatewayCanonicalRequest canonical = new GatewayCanonicalRequest(
                request.getMethod(),
                request.getRequestURI(),
                query(request),
                timestamp,
                nonce,
                request.body
        );
        if (!signer.matches(
                canonical.contentSha256(),
                header(request, GatewayRequestSigner.CONTENT_SHA256_HEADER)
        )) {
            throw failure("GATEWAY_REPORT_BODY_DIGEST_INVALID");
        }
        String secret;
        try {
            secret = protector.unprotect(
                    new GatewaySecretProtector.ProtectedSecret(
                            credential.secretCiphertext(),
                            credential.keyVersion()
                    ),
                    credential.applicationId() + ":" + accessKey
            );
        } catch (RuntimeException unavailable) {
            throw failure("GATEWAY_REPORT_CREDENTIAL_INVALID");
        }
        if (!signer.matches(
                signer.sign(canonical, secret),
                header(request, GatewayRequestSigner.SIGNATURE_HEADER)
        )) {
            throw failure("GATEWAY_REPORT_SIGNATURE_INVALID");
        }
        if (!nonces.claim(
                accessKey,
                nonce,
                now.plus(allowedSkew.multipliedBy(2)),
                now
        )) {
            throw new AuthenticationFailure(
                    409,
                    "GATEWAY_REPORT_REPLAYED"
            );
        }
        return new GatewayReportAuthentication(
                application.getId(),
                application.getBizCode(),
                application.getApplicationCode(),
                application.getEnv(),
                application.getNamespace(),
                accessKey
        );
    }

    /**
     * 中文说明：执行 active 操作；该方法是 {@code GatewayReportHmacFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code GatewayReportHmacFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param credential 参数 凭证；parameter credential。
     * @param now 参数 now；parameter now。
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    private boolean active(
            GatewayCredentialStore.CredentialRecord credential,
            Instant now) {
        return !"REVOKED".equals(credential.status())
                && !now.isBefore(credential.validFrom())
                && (credential.validUntil() == null
                || now.isBefore(credential.validUntil()));
    }

    /**
     * 中文说明：执行 timestamp 操作；该方法是 {@code GatewayReportHmacFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timestamp operation; this method is the invocation entry point on {@code GatewayReportHmacFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.timestamp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 timestamp 的处理结果；returns the result of the operation.
     */
    private long timestamp(HttpServletRequest request) {
        try {
            return Long.parseLong(header(
                    request,
                    GatewayRequestSigner.TIMESTAMP_HEADER
            ));
        } catch (NumberFormatException invalid) {
            throw failure("GATEWAY_REPORT_TIMESTAMP_INVALID");
        }
    }

    /**
     * 中文说明：执行 header 操作；该方法是 {@code GatewayReportHmacFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the header operation; this method is the invocation entry point on {@code GatewayReportHmacFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.header(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param name 参数 name；parameter name。
     * @return 返回 header 的处理结果；returns the result of the operation.
     */
    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw failure("GATEWAY_REPORT_HEADER_MISSING");
        }
        return value.trim();
    }

    /**
     * 中文说明：执行 query 操作；该方法是 {@code GatewayReportHmacFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query operation; this method is the invocation entry point on {@code GatewayReportHmacFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.query(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 query 的处理结果；returns the result of the operation.
     */
    private Map<String, List<String>> query(HttpServletRequest request) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, items) ->
                values.put(key, Arrays.asList(items)));
        return values;
    }

    /**
     * 中文说明：执行 failure 操作；该方法是 {@code GatewayReportHmacFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the failure operation; this method is the invocation entry point on {@code GatewayReportHmacFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param code 参数 code；parameter code。
     * @return 返回 failure 的处理结果；returns the result of the operation.
     */
    private AuthenticationFailure failure(String code) {
        return new AuthenticationFailure(401, code);
    }

    /**
     * 中文说明：{@code AuthenticationFailure} 是类型，位于当前 Gateway 模块的相关包中，负责AuthenticationFailure相关的职责与边界。
     * English summary: {@code AuthenticationFailure} is a type in the current Gateway module; it owns the authentication failure-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class AuthenticationFailure
            extends RuntimeException {

        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayReportHmacFilter.AuthenticationFailure} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code int}, and {@code GatewayReportHmacFilter.AuthenticationFailure} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter.AuthenticationFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter.AuthenticationFailure}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final int status;

        /**
         * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReportHmacFilter.AuthenticationFailure} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code GatewayReportHmacFilter.AuthenticationFailure} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter.AuthenticationFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter.AuthenticationFailure}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String code;

        /**
         * 中文说明：创建 {@code GatewayReportHmacFilter.AuthenticationFailure} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayReportHmacFilter.AuthenticationFailure} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param status 参数 status；parameter status。
         * @param code 参数 code；parameter code。
         */
        private AuthenticationFailure(int status, String code) {
            super("gateway report authentication failed");
            this.status = status;
            this.code = code;
        }
    }

    /**
     * 中文说明：{@code CachedBodyRequest} 是类型，位于当前 Gateway 模块的相关包中，负责CachedBody请求相关的职责与边界。
     * English summary: {@code CachedBodyRequest} is a type in the current Gateway module; it owns the cached body request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class CachedBodyRequest
            extends HttpServletRequestWrapper {

        /**
         * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code GatewayReportHmacFilter.CachedBodyRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code byte[]}, and {@code GatewayReportHmacFilter.CachedBodyRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportHmacFilter.CachedBodyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportHmacFilter.CachedBodyRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final byte[] body;

        /**
         * 中文说明：创建 {@code GatewayReportHmacFilter.CachedBodyRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayReportHmacFilter.CachedBodyRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param request 参数 请求；parameter request。
         */
        private CachedBodyRequest(HttpServletRequest request)
                throws IOException {
            super(request);
            body = request.getInputStream().readAllBytes();
        }

        /**
         * 中文说明：执行 getInputStream 操作；该方法是 {@code GatewayReportHmacFilter.CachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get input stream operation; this method is the invocation entry point on {@code GatewayReportHmacFilter.CachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.CachedBodyRequest.getInputStream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getInputStream 的处理结果；returns the result of the operation.
         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                /**
                 * 中文说明：执行 isFinished 操作；该方法是 {@code GatewayReportHmacFilter.CachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the is finished operation; this method is the invocation entry point on {@code GatewayReportHmacFilter.CachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.CachedBodyRequest.isFinished(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @return 返回 isFinished 的处理结果；returns the result of the operation.
                 */
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                /**
                 * 中文说明：执行 isReady 操作；该方法是 {@code GatewayReportHmacFilter.CachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the is ready operation; this method is the invocation entry point on {@code GatewayReportHmacFilter.CachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.CachedBodyRequest.isReady(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @return 返回 isReady 的处理结果；returns the result of the operation.
                 */
                @Override
                public boolean isReady() {
                    return true;
                }

                /**
                 * 中文说明：执行 setRead监听器 操作；该方法是 {@code GatewayReportHmacFilter.CachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the set read listener operation; this method is the invocation entry point on {@code GatewayReportHmacFilter.CachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.CachedBodyRequest.setReadListener(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param readListener 参数 read监听器；parameter read listener。
                 */
                @Override
                public void setReadListener(ReadListener readListener) {
                }

                /**
                 * 中文说明：执行 read 操作；该方法是 {@code GatewayReportHmacFilter.CachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the read operation; this method is the invocation entry point on {@code GatewayReportHmacFilter.CachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.CachedBodyRequest.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @return 返回 read 的处理结果；returns the result of the operation.
                 */
                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        /**
         * 中文说明：执行 getReader 操作；该方法是 {@code GatewayReportHmacFilter.CachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get reader operation; this method is the invocation entry point on {@code GatewayReportHmacFilter.CachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportHmacFilter.CachedBodyRequest.getReader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getReader 的处理结果；returns the result of the operation.
         */
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(
                    getInputStream(),
                    StandardCharsets.UTF_8
            ));
        }
    }
}
