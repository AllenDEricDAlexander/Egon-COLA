package top.egon.cola.component.ddc.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.api.RedissonClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.management.client.DdcClientTransportSecurity;
import top.egon.cola.component.ddc.management.client.DdcRestClientFactory;
import top.egon.cola.component.ddc.model.dto.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;
import top.egon.cola.component.ddc.trace.DdcTraceSupport;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 通过带签名和传输安全校验的 DDC OpenAPI 实现服务注册中心客户端。
 * / Service registry client implemented through the signed and transport-secured DDC OpenAPI.
 */
public final class DdcOpenApiServiceRegistryClient
        implements DdcServiceRegistryClient, AutoCloseable {

    /** 服务实例注册端点路径。 / Service instance registration endpoint path. */
    private static final String REGISTER_PATH =
            "/api/v1/ddc/openapi/registry/instances/register";

    /** 服务实例心跳端点路径。 / Service instance heartbeat endpoint path. */
    private static final String HEARTBEAT_PATH =
            "/api/v1/ddc/openapi/registry/instances/heartbeat";

    /** 服务实例注销端点路径。 / Service instance deregistration endpoint path. */
    private static final String DEREGISTER_PATH =
            "/api/v1/ddc/openapi/registry/instances/deregister";

    /** 服务实例查询端点路径。 / Service instance query endpoint path. */
    private static final String INSTANCES_PATH =
            "/api/v1/ddc/openapi/registry/instances";

    /** 服务目录查询端点路径。 / Service catalog query endpoint path. */
    private static final String SERVICES_PATH =
            "/api/v1/ddc/openapi/registry/services";

    /** DDC 客户端、安全和注册中心配置。 / DDC client, security, and registry properties. */
    private final DdcProperties properties;

    /** 调用 DDC Admin OpenAPI 的 HTTP 客户端。 / HTTP client used to call the DDC Admin OpenAPI. */
    private final RestClient restClient;

    /** 请求序列化及响应反序列化使用的映射器。 / Mapper used for request serialization and response deserialization. */
    private final ObjectMapper objectMapper;

    /** HMAC 请求签名器。 / HMAC request signer. */
    private final DdcRequestSigner signer = new DdcRequestSigner();

    /** 管理 Redis 事件和定时协调订阅的组件。 / Component managing Redis events and scheduled subscription reconciliation. */
    private final DdcRegistrySubscriptionManager subscriptionManager;

    /** 当前客户端创建的活跃注册索引。 / Index of active registrations created by this client. */
    private final DdcActiveRegistrationIndex registrations = new DdcActiveRegistrationIndex();

    /**
     * 创建 OpenAPI 注册中心客户端并校验 Admin 端点与传输安全配置。
     * / Creates an OpenAPI registry client and validates the Admin endpoint and transport security settings.
     *
     * @param properties DDC 客户端配置 / DDC client properties
     * @param redissonClient 用于接收注册中心事件的 Redisson 客户端 / Redisson client used to receive registry events
     * @throws IllegalArgumentException 端点协议与 mTLS 配置不一致时抛出
     * / if the endpoint scheme conflicts with the mTLS configuration
     * @throws IllegalStateException 必填端点或签名凭据缺失时抛出
     * / if the required endpoint or signing credentials are missing
     */
    public DdcOpenApiServiceRegistryClient(DdcProperties properties,
                                           RedissonClient redissonClient) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DdcProperties.Admin admin = properties.getAdmin();
        String endpoint = admin.requireEndpoint();
        admin.validateCredentials();
        DdcProperties.Tls tls = admin.getTls();
        DdcClientTransportSecurity transportSecurity =
                new DdcClientTransportSecurity(
                        tls.isEnabled(),
                        tls.isDevelopmentPlaintext(),
                        tls.getCertificateChainPath(),
                        tls.getPrivateKeyPath(),
                        tls.getTrustCertificateCollectionPath()
                );
        if (transportSecurity.enabled()
                && !endpoint.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "DDC mTLS endpoint must use HTTPS"
            );
        }
        if (!transportSecurity.enabled()
                && !endpoint.startsWith("http://")) {
            throw new IllegalArgumentException(
                    "DDC HTTPS endpoint requires configured mTLS"
            );
        }
        this.restClient = DdcRestClientFactory.create(
                        admin.getConnectTimeout(),
                        admin.getReadTimeout(),
                        transportSecurity
                )
                .baseUrl(endpoint)
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
        this.subscriptionManager = new DdcRegistrySubscriptionManager(
                this,
                redissonClient,
                properties.getRegistry().getReconcileIntervalSeconds()
        );
    }

    /**
     * 向 Admin 注册服务实例并在本地记录返回的租约。
     * / Registers a service instance with Admin and records the returned lease locally.
     *
     * @param registration 服务注册信息 / service registration
     * @return 已校验的租约会话 / validated lease session
     * @throws DdcException 响应会话为空、角色或实例不匹配，或远端返回失败时抛出
     * / if the response session is absent, has a mismatched role or instance, or the remote call fails
     */
    @Override
    public DdcLeaseSession register(DdcServiceRegistration registration) {
        DdcLeaseSession session = post(
                REGISTER_PATH,
                registration,
                new ParameterizedTypeReference<>() {
                }
        );
        if (session == null
                || session.role() != registration.serviceKey().serviceKind().leaseRole()
                || !registration.instanceId().equals(session.instanceId())) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE);
        }
        registrations.put(registration.serviceKey(), session);
        return session;
    }

    /**
     * 续期当前客户端已记录的实例租约。
     * / Renews an instance lease recorded by this client.
     *
     * @param instanceId 实例标识 / instance identifier
     * @param leaseId 租约标识 / lease identifier
     * @return 租约续期结果 / lease renewal result
     * @throws DdcException 本地租约不匹配或远端返回失败时抛出
     * / if the local lease does not match or the remote call fails
     */
    @Override
    public DdcLeaseOperationResult heartbeat(String instanceId, String leaseId) {
        DdcServiceKey serviceKey = registrations.require(instanceId, leaseId);
        DdcLeaseOperationResult result = post(
                HEARTBEAT_PATH,
                leaseRequest(instanceId, leaseId, serviceKey),
                new ParameterizedTypeReference<>() {
                }
        );
        if (result.status() == DdcLeaseOperationStatus.NOT_FOUND
                || result.status() == DdcLeaseOperationStatus.LEASE_MISMATCH) {
            registrations.remove(leaseId);
        }
        return result;
    }

    /**
     * 注销当前客户端已记录的实例租约。
     * / Deregisters an instance lease recorded by this client.
     *
     * @param instanceId 实例标识 / instance identifier
     * @param leaseId 租约标识 / lease identifier
     * @return 租约注销结果 / lease deregistration result
     * @throws DdcException 本地租约不匹配或远端返回失败时抛出
     * / if the local lease does not match or the remote call fails
     */
    @Override
    public DdcLeaseOperationResult deregister(String instanceId, String leaseId) {
        DdcServiceKey serviceKey = registrations.require(instanceId, leaseId);
        DdcLeaseOperationResult result = post(
                DEREGISTER_PATH,
                leaseRequest(instanceId, leaseId, serviceKey),
                new ParameterizedTypeReference<>() {
                }
        );
        if (result.status() != DdcLeaseOperationStatus.NOT_DELETED) {
            registrations.remove(leaseId);
        }
        return result;
    }

    /**
     * 从 Admin 获取指定服务键的实例快照。
     * / Fetches the instance snapshot for a service key from Admin.
     *
     * @param serviceKey 服务键 / service key
     * @return 服务实例快照 / service instance snapshot
     * @throws DdcException 远端返回失败或缺少数据时抛出 / if the remote response fails or has no data
     */
    @Override
    public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
        Map<String, List<String>> query = serviceKeyQuery(serviceKey);
        return get(
                INSTANCES_PATH,
                query,
                new ParameterizedTypeReference<>() {
                }
        );
    }

    /**
     * 订阅指定服务键的实例快照变化。
     * / Subscribes to instance snapshot changes for a service key.
     *
     * @param serviceKey 服务键 / service key
     * @param listener 快照监听器 / snapshot listener
     * @return 可关闭订阅句柄 / closeable subscription handle
     */
    @Override
    public DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener) {
        return subscriptionManager.subscribe(serviceKey, listener);
    }

    /**
     * 从 Admin 获取匹配查询条件的服务目录快照。
     * / Fetches the service catalog snapshot matching a query from Admin.
     *
     * @param query 服务目录查询 / service catalog query
     * @return 服务目录快照 / service catalog snapshot
     * @throws DdcException 远端返回失败或缺少数据时抛出 / if the remote response fails or has no data
     */
    @Override
    public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query) {
        return get(
                SERVICES_PATH,
                serviceQuery(query),
                new ParameterizedTypeReference<>() {
                }
        );
    }

    /**
     * 将非空服务查询字段转换为 OpenAPI 查询参数。
     * / Converts non-null service query fields to OpenAPI query parameters.
     *
     * @param query 服务目录查询 / service catalog query
     * @return 保持规范参数顺序的查询参数 / query parameters preserving canonical parameter order
     */
    static Map<String, List<String>> serviceQuery(DdcServiceQuery query) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        putIfPresent(parameters, "bizCode", query.bizCode());
        putIfPresent(parameters, "appCode", query.appCode());
        putIfPresent(parameters, "env", query.env());
        if (query.serviceKind() != null) {
            parameters.put("serviceKind", List.of(query.serviceKind().name()));
        }
        putIfPresent(parameters, "protocol", query.protocol());
        putIfPresent(parameters, "serviceName", query.serviceName());
        putIfPresent(parameters, "group", query.group());
        putIfPresent(parameters, "version", query.version());
        return parameters;
    }

    /**
     * 订阅匹配查询条件的服务目录变化。
     * / Subscribes to service catalog changes matching a query.
     *
     * @param query 包含精确主题范围的服务查询 / service query containing an exact topic scope
     * @param listener 服务目录监听器 / service catalog listener
     * @return 可关闭订阅句柄 / closeable subscription handle
     * @throws IllegalArgumentException 查询缺少精确主题范围时抛出 / if the query lacks an exact topic scope
     */
    @Override
    public DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener) {
        return subscriptionManager.subscribeServices(query, listener);
    }

    /**
     * 关闭全部订阅并清除本地活跃注册索引。
     * / Closes all subscriptions and clears the local active-registration index.
     */
    @Override
    public void close() {
        subscriptionManager.close();
        registrations.clear();
    }

    /**
     * 构造携带服务键的租约操作请求。
     * / Builds a lease operation request carrying its service key.
     *
     * @param instanceId 实例标识 / instance identifier
     * @param leaseId 租约标识 / lease identifier
     * @param serviceKey 租约所属服务键 / service key that owns the lease
     * @return 租约操作请求 / lease operation request
     */
    private DdcServiceLeaseRequest leaseRequest(String instanceId,
                                                String leaseId,
                                                DdcServiceKey serviceKey) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(serviceKey);
        request.setInstanceId(instanceId);
        request.setLeaseId(leaseId);
        return request;
    }

    /**
     * 将完整服务键转换为实例查询参数。
     * / Converts a complete service key to instance query parameters.
     *
     * @param serviceKey 服务键 / service key
     * @return 保持规范参数顺序的查询参数 / query parameters preserving canonical parameter order
     */
    static Map<String, List<String>> serviceKeyQuery(DdcServiceKey serviceKey) {
        Map<String, List<String>> query = new LinkedHashMap<>();
        query.put("bizCode", List.of(serviceKey.bizCode()));
        query.put("appCode", List.of(serviceKey.appCode()));
        query.put("env", List.of(serviceKey.env()));
        query.put("serviceKind", List.of(serviceKey.serviceKind().name()));
        query.put("serviceName", List.of(serviceKey.serviceName()));
        putIfPresent(query, "group", serviceKey.group());
        putIfPresent(query, "version", serviceKey.version());
        query.put("protocol", List.of(serviceKey.protocol()));
        return query;
    }

    /**
     * 在值非空白时添加单值查询参数。
     * / Adds a single-valued query parameter when its value is non-blank.
     *
     * @param target 目标参数映射 / target parameter map
     * @param name 参数名 / parameter name
     * @param value 参数值 / parameter value
     */
    private static void putIfPresent(Map<String, List<String>> target,
                                     String name,
                                     String value) {
        if (value != null && !value.isBlank()) {
            target.put(name, List.of(value));
        }
    }

    /**
     * 发送带规范查询和安全请求头的 GET 请求。
     * / Sends a GET request with a canonical query and security headers.
     *
     * @param path OpenAPI 路径 / OpenAPI path
     * @param query 查询参数 / query parameters
     * @param responseType 结果包装的泛型类型 / generic type of the result wrapper
     * @param <T> 响应数据类型 / response data type
     * @return 已解包且非空的响应数据 / unwrapped non-null response data
     * @throws DdcException 响应失败、为空或缺少数据时抛出 / if the response fails, is null, or has no data
     */
    private <T> T get(String path,
                      Map<String, List<String>> query,
                      ParameterizedTypeReference<ResultRecord<T>> responseType) {
        DdcCanonicalRequest canonicalRequest =
                canonicalRequest("GET", path, query, new byte[0]);
        ResultRecord<T> result = restClient.get()
                .uri(URI.create(path + "?" + canonicalRequest.canonicalQuery()))
                .headers(headers -> prepareHeaders(headers, canonicalRequest))
                .retrieve()
                .body(responseType);
        return data(result);
    }

    /**
     * 序列化请求并发送带安全请求头的 POST 请求。
     * / Serializes a request and sends a POST request with security headers.
     *
     * @param path OpenAPI 路径 / OpenAPI path
     * @param request 请求对象 / request object
     * @param responseType 结果包装的泛型类型 / generic type of the result wrapper
     * @param <T> 响应数据类型 / response data type
     * @return 已解包且非空的响应数据 / unwrapped non-null response data
     * @throws DdcException 序列化或远端响应处理失败时抛出 / if serialization or remote response handling fails
     */
    private <T> T post(String path,
                       Object request,
                       ParameterizedTypeReference<ResultRecord<T>> responseType) {
        byte[] body = serialize(request);
        DdcCanonicalRequest canonicalRequest =
                canonicalRequest("POST", path, Map.of(), body);
        ResultRecord<T> result = restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> prepareHeaders(headers, canonicalRequest))
                .body(body)
                .retrieve()
                .body(responseType);
        return data(result);
    }

    /**
     * 将请求对象序列化为 JSON 字节。
     * / Serializes a request object to JSON bytes.
     *
     * @param request 请求对象 / request object
     * @return JSON 请求体 / JSON request body
     * @throws DdcException Jackson 无法序列化请求时抛出 / if Jackson cannot serialize the request
     */
    private byte[] serialize(Object request) {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE, exception);
        }
    }

    /**
     * 使用当前时间、随机 nonce 和请求体构造规范签名请求。
     * / Builds a canonical signing request with the current time, a random nonce, and the body.
     *
     * @param method HTTP 方法 / HTTP method
     * @param path 请求路径 / request path
     * @param query 查询参数 / query parameters
     * @param body 请求体字节 / request body bytes
     * @return 规范请求 / canonical request
     */
    private DdcCanonicalRequest canonicalRequest(String method,
                                                 String path,
                                                 Map<String, List<String>> query,
                                                 byte[] body) {
        return new DdcCanonicalRequest(
                method,
                path,
                query,
                System.currentTimeMillis(),
                UUID.randomUUID().toString(),
                body
        );
    }

    /**
     * 在启用签名时向请求头写入访问密钥、时间戳、nonce、内容摘要和签名。
     * / Writes the access key, timestamp, nonce, content digest, and signature to headers when signing is enabled.
     *
     * @param headers 待补充的请求头 / headers to populate
     * @param request 规范请求 / canonical request
     */
    private void sign(HttpHeaders headers, DdcCanonicalRequest request) {
        if (!properties.getAdmin().isSignatureEnabled()) {
            return;
        }
        headers.set(DdcRequestSigner.ACCESS_KEY_HEADER, properties.getAdmin().getAccessKey());
        headers.set(DdcRequestSigner.TIMESTAMP_HEADER, Long.toString(request.timestamp()));
        headers.set(DdcRequestSigner.NONCE_HEADER, request.nonce());
        headers.set(DdcRequestSigner.CONTENT_SHA256_HEADER, request.contentSha256());
        headers.set(
                DdcRequestSigner.SIGNATURE_HEADER,
                signer.sign(request, properties.getAdmin().getSecretKey())
        );
    }

    /**
     * 注入追踪上下文并按配置签名请求。
     * / Injects trace context and signs the request according to configuration.
     *
     * @param headers 待补充的请求头 / headers to populate
     * @param request 规范请求 / canonical request
     */
    private void prepareHeaders(HttpHeaders headers, DdcCanonicalRequest request) {
        DdcTraceSupport.inject(headers);
        sign(headers, request);
    }

    /**
     * 校验统一结果包装并提取非空数据。
     * / Validates the common result wrapper and extracts non-null data.
     *
     * @param result 统一结果包装 / common result wrapper
     * @param <T> 响应数据类型 / response data type
     * @return 非空响应数据 / non-null response data
     * @throws DdcException 结果为空、失败或数据为空时抛出 / if the result is null, unsuccessful, or has null data
     */
    private <T> T data(ResultRecord<T> result) {
        if (result == null) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE);
        }
        if (!result.success()) {
            throw new DdcException(result.code(), result.status(), result.message());
        }
        if (result.data() == null) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE);
        }
        return result.data();
    }

}
