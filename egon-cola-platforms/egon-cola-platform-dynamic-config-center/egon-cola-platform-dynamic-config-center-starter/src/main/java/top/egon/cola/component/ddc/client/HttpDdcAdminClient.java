package top.egon.cola.component.ddc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.transport.http.DdcClientTransportSecurity;
import top.egon.cola.component.ddc.transport.http.DdcOpenApiRequestException;
import top.egon.cola.component.ddc.transport.http.DdcOpenApiRequestFactory;
import top.egon.cola.component.ddc.transport.http.DdcRestClientFactory;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过带追踪信息和可选 HMAC 签名的 HTTP 请求实现 DDC 管理端客户端。 Implements the DDC management client over HTTP with trace propagation and optional HMAC signing.
 */
public class HttpDdcAdminClient implements DdcAdminClient {

    /**
     * 当前客户端的作用域、管理端和安全属性。 Scope, management endpoint, and security properties for this client.
     */
    private final DdcProperties properties;

    /**
     * 用于调用 DDC OpenAPI 的 Spring REST 客户端。 Spring REST client used for DDC OpenAPI calls.
     */
    private final RestClient restClient;

    /**
     * 统一构造规范目标、追踪头、可选签名和 JSON 请求体。 Builds canonical targets, trace headers, optional signatures, and JSON bodies.
     */
    private final DdcOpenApiRequestFactory requestFactory;

    /**
     * 根据属性创建具备超时和传输安全配置的 HTTP 客户端。 Creates an HTTP client with timeout and transport-security settings from the properties.
     *
     * @param properties DDC 客户端属性。 DDC client properties
     * @throws IllegalArgumentException 管理端地址、凭据或 TLS 组合无效时抛出。 thrown when the endpoint, credentials, or TLS combination is invalid
     */
    public HttpDdcAdminClient(DdcProperties properties) {
        this(properties, restClientBuilder(properties));
    }

    /**
     * 创建符合管理端 TLS 和超时配置的 REST 客户端构建器。 Creates a REST client builder matching management TLS and timeout settings.
     *
     * @param properties DDC 客户端属性。 DDC client properties
     * @return 已配置但尚未绑定基础地址的构建器。 configured builder not yet bound to a base URL
     * @throws IllegalArgumentException TLS 状态与端点协议不匹配时抛出。 thrown when TLS state and endpoint scheme do not match
     */
    private static RestClient.Builder restClientBuilder(
            DdcProperties properties) {
        DdcProperties.Admin admin = properties.getAdmin();
        String endpoint = admin.requireEndpoint();
        admin.validateCredentials();
        DdcProperties.Tls tls = admin.getTls();
        DdcClientTransportSecurity security =
                new DdcClientTransportSecurity(
                        tls.isEnabled(),
                        tls.isDevelopmentPlaintext(),
                        tls.getCertificateChainPath(),
                        tls.getPrivateKeyPath(),
                        tls.getTrustCertificateCollectionPath()
                );
        if (security.enabled()
                && !endpoint.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "DDC mTLS endpoint must use HTTPS"
            );
        }
        if (!security.enabled()
                && !endpoint.startsWith("http://")) {
            throw new IllegalArgumentException(
                    "DDC HTTPS endpoint requires configured mTLS"
            );
        }
        return DdcRestClientFactory.create(
                admin.getConnectTimeout(),
                admin.getReadTimeout(),
                security
        );
    }

    /**
     * 使用指定 REST 构建器创建客户端，供包内测试替换传输层。 Creates a client with the supplied REST builder so package-level tests can replace transport.
     *
     * @param properties        DDC 客户端属性。 DDC client properties
     * @param restClientBuilder REST 客户端构建器。 REST client builder
     */
    HttpDdcAdminClient(DdcProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        DdcProperties.Admin admin = properties.getAdmin();
        String endpoint = admin.requireEndpoint();
        admin.validateCredentials();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.requestFactory = new DdcOpenApiRequestFactory(
                objectMapper,
                admin.isSignatureEnabled(),
                admin.getAccessKey(),
                admin.getSecretKey()
        );
        this.restClient = restClientBuilder
                .baseUrl(endpoint)
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }

    /**
     * 调用实例注册接口并要求响应包含租约会话。 Calls instance registration and requires a lease session in the response.
     *
     * @param request 实例注册请求。 instance registration request
     * @return 管理端返回的租约会话。 lease session returned by the management service
     */
    @Override
    public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
        return post(
                "/api/v1/ddc/openapi/instances/register",
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 调用实例心跳接口并要求响应包含操作结果。 Calls the instance heartbeat endpoint and requires an operation result.
     *
     * @param request 实例心跳请求。 instance heartbeat request
     * @return 管理端返回的租约操作结果。 lease operation result returned by the management service
     */
    @Override
    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
        return post(
                "/api/v1/ddc/openapi/instances/heartbeat",
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 调用实例下线接口并要求响应包含操作结果。 Calls the instance offline endpoint and requires an operation result.
     *
     * @param request 实例下线请求。 instance offline request
     * @return 管理端返回的租约操作结果。 lease operation result returned by the management service
     */
    @Override
    public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
        return post(
                "/api/v1/ddc/openapi/instances/offline",
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 以客户端业务、应用和环境参数拉取配置。 Pulls configuration using the client's business, application, and environment parameters.
     *
     * @return 管理端返回的配置列表，响应数据为空时返回空列表。 configurations returned by the service, or an empty list when response data is null
     */
    @Override
    public List<DdcConfigValue> pull() {
        String path = "/api/v1/ddc/openapi/configs/pull";
        Map<String, List<String>> query = new LinkedHashMap<>();
        query.put("bizCode", List.of(properties.getBizCode()));
        query.put("appCode", List.of(properties.getAppCode()));
        query.put("env", List.of(properties.getEnv()));
        DdcOpenApiRequestFactory.Request request = request(
                HttpMethod.GET,
                path,
                query,
                null
        );
        ResultRecord<List<DdcConfigValue>> result = restClient.get()
                .uri(request.target())
                .headers(headers -> headers.addAll(request.headers()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        List<DdcConfigValue> values = data(result, false);
        return values == null ? Collections.emptyList() : values;
    }

    /**
     * 提交发布确认；成功响应不要求携带数据。 Submits a publication acknowledgement without requiring response data.
     *
     * @param request 发布确认请求。 publication acknowledgement request
     */
    @Override
    public void ack(DdcAckRequest request) {
        post(
                "/api/v1/ddc/openapi/publish/ack",
                request,
                new ParameterizedTypeReference<ResultRecord<Void>>() {
                },
                false
        );
    }

    /**
     * 序列化请求、创建规范签名上下文并执行 POST 调用。 Serializes a request, creates its canonical signing context, and executes a POST call.
     *
     * @param path         OpenAPI 相对路径。 OpenAPI relative path
     * @param request      请求对象。 request object
     * @param responseType ResultRecord 响应类型。 ResultRecord response type
     * @param required     成功响应是否必须包含数据。 whether a successful response must contain data
     * @param <T>          响应数据类型。 response data type
     * @return 成功响应中的数据。 data from the successful response
     * @throws DdcException 序列化失败、远端失败或必需数据缺失时抛出。 thrown on serialization failure, remote failure, or missing required data
     */
    private <T> T post(String path,
                       Object request,
                       ParameterizedTypeReference<ResultRecord<T>> responseType,
                       boolean required) {
        DdcOpenApiRequestFactory.Request openApiRequest = request(
                HttpMethod.POST,
                path,
                Map.of(),
                request
        );
        ResultRecord<T> result = restClient.post()
                .uri(openApiRequest.target())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.addAll(openApiRequest.headers()))
                .body(openApiRequest.body())
                .retrieve()
                .body(responseType);
        return data(result, required);
    }

    /**
     * 校验统一响应并提取数据，将失败状态映射为 DDC 异常。 Validates a common response and extracts data, mapping failure status to a DDC exception.
     *
     * @param result   远端统一响应。 remote common response
     * @param required 是否要求非空数据。 whether non-null data is required
     * @param <T>      响应数据类型。 response data type
     * @return 响应数据。 response data
     * @throws DdcException 响应缺失、失败或必需数据缺失时抛出。 thrown when the response is absent, failed, or lacks required data
     */
    private <T> T data(ResultRecord<T> result, boolean required) {
        if (result == null) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE);
        }
        if (!result.success()) {
            throw new DdcException(result.code(), result.status(), result.message());
        }
        if (required && result.data() == null) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE);
        }
        return result.data();
    }

    /**
     * 构造 OpenAPI 请求并将构造失败映射为 DDC 异常。 Builds an OpenAPI request and maps construction failures to DDC exceptions.
     *
     * @param method HTTP 方法。 HTTP method
     * @param path   请求路径。 request path
     * @param query  查询参数。 query parameters
     * @param body   可选请求体。 optional request body
     * @return 已准备的 OpenAPI 请求。 prepared OpenAPI request
     * @throws DdcException JSON 序列化失败时抛出。 thrown when JSON serialization fails
     */
    private DdcOpenApiRequestFactory.Request request(
            HttpMethod method,
            String path,
            Map<String, List<String>> query,
            Object body) {
        try {
            return requestFactory.create(method, path, query, body);
        } catch (DdcOpenApiRequestException exception) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE, exception);
        }
    }
}
