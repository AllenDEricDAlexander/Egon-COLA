package top.egon.cola.component.ddc.management.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.management.model.DdcManagementScopeQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;
import top.egon.cola.component.ddc.transport.http.DdcOpenApiRequestException;
import top.egon.cola.component.ddc.transport.http.DdcOpenApiRequestFactory;
import top.egon.cola.component.ddc.transport.http.DdcRestClientFactory;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 通过 HMAC 签名的 DDC 管理 OpenAPI 实现 {@link DdcManagementClient}。 /
 * {@link DdcManagementClient} implementation backed by the HMAC-signed DDC management OpenAPI.
 */
public final class HttpDdcManagementClient implements DdcManagementClient {

    /**
     * DDC 管理开放接口的固定基础路径。 / Fixed base path of the DDC management OpenAPI.
     */
    private static final String MANAGEMENT_PATH = "/api/v1/ddc/openapi/management";

    /**
     * 执行管理 HTTP 请求的 Spring REST 客户端。 / Spring REST client that executes management HTTP requests.
     */
    private final RestClient restClient;

    /**
     * 创建规范 URI、请求体和 HMAC 认证头的请求工厂。 / Factory that creates canonical URIs, bodies, and HMAC authentication headers.
     */
    private final DdcOpenApiRequestFactory requestFactory;

    /**
     * 使用生产默认时钟、随机防重放值与按配置创建的 REST 传输构造客户端。 /
     * Constructs a client with the production clock, random nonces, and REST transport created from settings.
     *
     * @param properties 客户端地址、认证、超时与传输安全配置 / endpoint, authentication, timeout, and transport-security settings
     * @throws IllegalArgumentException 当配置为空时 / when settings are null
     */
    public HttpDdcManagementClient(DdcManagementClientProperties properties) {
        this(
                properties,
                defaultBuilder(properties),
                Clock.systemUTC(),
                () -> UUID.randomUUID().toString()
        );
    }

    /**
     * 使用可注入的 REST 构建器、时钟与防重放值供应器构造客户端，供包内测试使用。 /
     * Constructs a client with injectable REST builder, clock, and nonce supplier for package-level testing.
     *
     * @param properties        客户端地址与认证配置 / client endpoint and authentication settings
     * @param restClientBuilder REST 客户端构建器 / REST client builder
     * @param clock             生成签名时间戳的时钟 / clock used for signature timestamps
     * @param nonceSupplier     生成防重放值的供应器 / supplier used for anti-replay nonces
     * @throws IllegalArgumentException 当任一依赖为空时 / when any dependency is null
     */
    HttpDdcManagementClient(
            DdcManagementClientProperties properties,
            RestClient.Builder restClientBuilder,
            Clock clock,
            Supplier<String> nonceSupplier
    ) {
        require(properties, "properties");
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.requestFactory = new DdcOpenApiRequestFactory(
                objectMapper,
                require(clock, "clock"),
                require(nonceSupplier, "nonceSupplier"),
                true,
                properties.accessKey(),
                properties.secretKey()
        );
        this.restClient = require(restClientBuilder, "restClientBuilder")
                .baseUrl(properties.endpoint())
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }

    /**
     * 精确查询配置，并将服务端“配置不存在”业务码转换为空结果。 /
     * Looks up an exact configuration and maps the server's "configuration not found" business code to an empty result.
     *
     * @param query 完整配置作用域 / complete configuration scope
     * @return 找到的配置，未找到时为空 / matching configuration, or empty when none exists
     * @throws DdcManagementClientException 当服务端返回其他错误或请求失败时 / when the server returns another error or the request fails
     */
    @Override
    public Optional<DdcManagementConfig> findConfig(DdcManagementConfigQuery query) {
        require(query, "query");
        try {
            return Optional.of(exchange(
                    HttpMethod.GET,
                    configPath(
                            query.bizCode(),
                            query.env(),
                            query.appCode()
                    ),
                    Map.of(),
                    null,
                    new ParameterizedTypeReference<>() {
                    },
                    true
            ));
        } catch (DdcManagementClientException exception) {
            if (exception.code() == DdcManagementErrorCode.CONFIG_NOT_FOUND.getCode()) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    /**
     * 新增或更新指定作用域的配置。 / Creates or updates the configuration in the requested scope.
     *
     * @param request 配置写入请求 / configuration upsert request
     * @return 服务端保存后的配置 / configuration persisted by the server
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request) {
        require(request, "request");
        return exchange(
                HttpMethod.PUT,
                configPath(
                        request.bizCode(),
                        request.env(),
                        request.appCode()
                ),
                Map.of(),
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 删除指定作用域的配置。 / Deletes the configuration in the requested scope.
     *
     * @param request 配置删除请求 / configuration deletion request
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public void delete(DdcManagementConfigDeleteRequest request) {
        require(request, "request");
        exchange(
                HttpMethod.DELETE,
                configPath(
                        request.bizCode(),
                        request.env(),
                        request.appCode()
                ),
                Map.of(),
                request,
                new ParameterizedTypeReference<ResultRecord<Void>>() {
                },
                false
        );
    }

    /**
     * 创建并分发配置发布任务。 / Creates and dispatches a configuration publication task.
     *
     * @param request 配置发布请求 / configuration publication request
     * @return 发布受理或执行结果 / publication acceptance or execution result
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public DdcManagementPublishResult publish(DdcManagementPublishRequest request) {
        require(request, "request");
        return exchange(
                HttpMethod.POST,
                configPath(
                        request.bizCode(),
                        request.env(),
                        request.appCode()
                ) + "/publish",
                Map.of(),
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 查询指定变更标识的发布任务。 / Retrieves the publication task for a change identifier.
     *
     * @param changeId 发布变更标识 / publication change identifier
     * @return 发布任务详情 / publication task details
     * @throws IllegalArgumentException     当变更标识为空时 / when the change identifier is blank
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public DdcManagementPublishTask getPublishTask(String changeId) {
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/publish-tasks/" + segment(changeId, "changeId"),
                Map.of(),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 重试指定变更标识的发布任务。 / Retries the publication task for a change identifier.
     *
     * @param changeId 发布变更标识 / publication change identifier
     * @return 重试后的发布结果 / publication result after retrying
     * @throws IllegalArgumentException     当变更标识为空时 / when the change identifier is blank
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public DdcManagementPublishResult retry(String changeId) {
        return exchange(
                HttpMethod.POST,
                MANAGEMENT_PATH + "/publish-tasks/" + segment(changeId, "changeId") + "/retry",
                Map.of(),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 查询匹配作用域条件的配置客户端实例。 / Lists configuration-client instances matching scope filters.
     *
     * @param query 配置客户端筛选条件 / configuration-client filters
     * @return 匹配的配置客户端实例 / matching configuration-client instances
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public List<DdcManagementConfigClientInstance> getConfigClients(
            DdcManagementInstanceQuery query
    ) {
        require(query, "query");
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/instances",
                configClientQuery(query),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 查询匹配条件的命名空间、环境与应用作用域绑定。 /
     * Lists namespace, environment, and application scope bindings matching the filters.
     *
     * @param query 作用域绑定筛选条件 / scope-binding filters
     * @return 匹配的作用域绑定 / matching scope bindings
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public List<DdcManagementScopeBinding> getScopeBindings(
            DdcManagementScopeQuery query) {
        require(query, "query");
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/scope-bindings",
                scopeQuery(query),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 查询匹配的服务键目录。 / Retrieves the catalog of matching service keys.
     *
     * @param query 服务筛选条件 / service filters
     * @return 服务键目录 / service-key catalog
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public DdcManagementServiceCatalog getServiceKeys(DdcManagementServiceQuery query) {
        require(query, "query");
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/registry/services",
                serviceQuery(query),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 查询指定服务的实例快照。 / Retrieves the instance snapshot for a service.
     *
     * @param query 服务筛选与定位条件 / service filters and identity criteria
     * @return 服务实例快照 / service-instance snapshot
     * @throws DdcManagementClientException 当服务端拒绝或请求失败时 / when the server rejects the operation or the request fails
     */
    @Override
    public DdcManagementServiceSnapshot getInstances(DdcManagementServiceQuery query) {
        require(query, "query");
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/registry/instances",
                serviceQuery(query),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    /**
     * 签名并执行管理请求，然后解包统一结果信封。 /
     * Signs and executes a management request, then unwraps the common result envelope.
     *
     * @param <T>          响应数据类型 / response-data type
     * @param method       HTTP 方法 / HTTP method
     * @param path         管理接口路径 / management API path
     * @param query        查询参数 / query parameters
     * @param request      可选请求对象 / optional request object
     * @param responseType 统一结果信封的参数化类型 / parameterized type of the common result envelope
     * @param required     成功响应是否必须包含数据 / whether a successful response must contain data
     * @return 已解包的响应数据 / unwrapped response data
     * @throws DdcManagementClientException 当签名、传输或业务响应失败时 / when signing, transport, or the business response fails
     */
    private <T> T exchange(
            HttpMethod method,
            String path,
            Map<String, List<String>> query,
            Object request,
            ParameterizedTypeReference<ResultRecord<T>> responseType,
            boolean required
    ) {
        try {
            DdcOpenApiRequestFactory.Request openApiRequest =
                    requestFactory.create(method, path, query, request);
            RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(openApiRequest.target())
                    .headers(headers -> headers.addAll(openApiRequest.headers()));
            if (openApiRequest.hasBody()) {
                spec.contentType(MediaType.APPLICATION_JSON).body(openApiRequest.body());
            }
            ResultRecord<T> result = spec.retrieve().body(responseType);
            return data(result, required);
        } catch (DdcOpenApiRequestException exception) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_SERIALIZATION_ERROR",
                    "DDC management request serialization failed",
                    exception
            );
        } catch (DdcManagementClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_IO_ERROR",
                    "DDC management request failed",
                    exception
            );
        }
    }

    /**
     * 校验统一结果信封并返回其数据。 / Validates a common result envelope and returns its data.
     *
     * @param <T>      响应数据类型 / response-data type
     * @param result   服务端结果信封 / server result envelope
     * @param required 成功结果是否必须包含数据 / whether successful results must contain data
     * @return 结果数据；允许空数据时可能为空 / result data, possibly null when empty data is permitted
     * @throws DdcManagementClientException 当响应为空、业务失败或缺少必需数据时 / when the response is empty, reports failure, or lacks required data
     */
    private <T> T data(ResultRecord<T> result, boolean required) {
        if (result == null) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_EMPTY_RESPONSE",
                    "DDC management returned an empty response",
                    null
            );
        }
        if (!result.success()) {
            throw new DdcManagementClientException(
                    result.code(),
                    result.status(),
                    result.message()
            );
        }
        if (required && result.data() == null) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_EMPTY_DATA",
                    "DDC management returned empty data",
                    null
            );
        }
        return result.data();
    }

    /**
     * 构建并编码完整配置作用域的资源路径。 / Builds and encodes the resource path for a complete configuration scope.
     *
     * @param bizCode 业务编码 / business code
     * @param env     环境编码 / environment code
     * @param appCode 应用编码 / application code
     * @return 配置资源路径 / configuration resource path
     * @throws IllegalArgumentException 当任一路径段为空时 / when any path segment is blank
     */
    private String configPath(
            String bizCode,
            String env,
            String appCode
    ) {
        return MANAGEMENT_PATH
                + "/configs/"
                + segment(bizCode, "bizCode") + "/"
                + segment(env, "env") + "/"
                + segment(appCode, "appCode");
    }

    /**
     * 将配置客户端筛选条件投影为非空查询参数。 /
     * Projects configuration-client filters into nonblank query parameters.
     *
     * @param query 配置客户端筛选条件 / configuration-client filters
     * @return 保持声明顺序的查询参数 / query parameters preserving declaration order
     */
    private Map<String, List<String>> configClientQuery(DdcManagementInstanceQuery query) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        putQuery(values, "bizCode", query.bizCode());
        putQuery(values, "env", query.env());
        putQuery(values, "appCode", query.appCode());
        return values;
    }

    /**
     * 将服务筛选条件投影为非空查询参数。 / Projects service filters into nonblank query parameters.
     *
     * @param query 服务筛选条件 / service filters
     * @return 保持声明顺序的查询参数 / query parameters preserving declaration order
     */
    private Map<String, List<String>> serviceQuery(DdcManagementServiceQuery query) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        putQuery(values, "bizCode", query.bizCode());
        putQuery(values, "namespaceCode", query.namespaceCode());
        putQuery(values, "env", query.env());
        putQuery(values, "appCode", query.appCode());
        putQuery(values, "serviceKind", query.serviceKind());
        putQuery(values, "protocol", query.protocol());
        putQuery(values, "serviceName", query.serviceName());
        putQuery(values, "group", query.group());
        putQuery(values, "version", query.version());
        return values;
    }

    /**
     * 将作用域绑定筛选条件投影为非空查询参数。 /
     * Projects scope-binding filters into nonblank query parameters.
     *
     * @param query 作用域绑定筛选条件 / scope-binding filters
     * @return 保持声明顺序的查询参数 / query parameters preserving declaration order
     */
    private Map<String, List<String>> scopeQuery(DdcManagementScopeQuery query) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        putQuery(values, "bizCode", query.bizCode());
        putQuery(values, "namespaceCode", query.namespaceCode());
        putQuery(values, "env", query.env());
        putQuery(values, "appCode", query.appCode());
        return values;
    }

    /**
     * 仅在值非空时添加单值查询参数。 / Adds a single-valued query parameter only when its value is nonblank.
     *
     * @param query 目标查询参数映射 / target query-parameter map
     * @param name  参数名 / parameter name
     * @param value 参数值 / parameter value
     */
    private void putQuery(Map<String, List<String>> query, String name, String value) {
        if (value != null && !value.isBlank()) {
            query.put(name, List.of(value));
        }
    }

    /**
     * 校验并按 UTF-8 编码单个 URI 路径段。 / Validates and UTF-8-encodes one URI path segment.
     *
     * @param value     路径段原值 / raw path-segment value
     * @param fieldName 用于错误消息的字段名 / field name used in error messages
     * @return 编码后的路径段 / encoded path segment
     * @throws IllegalArgumentException 当路径段为空时 / when the path segment is blank
     */
    private String segment(String value, String fieldName) {
        return UriUtils.encodePathSegment(
                requireText(value, fieldName),
                StandardCharsets.UTF_8
        );
    }

    /**
     * 根据客户端超时与传输安全配置创建默认 REST 构建器。 /
     * Creates the default REST builder from client timeout and transport-security settings.
     *
     * @param properties 客户端配置 / client settings
     * @return 默认 REST 客户端构建器 / default REST client builder
     * @throws IllegalArgumentException 当配置为空时 / when settings are null
     */
    private static RestClient.Builder defaultBuilder(
            DdcManagementClientProperties properties
    ) {
        require(properties, "properties");
        return DdcRestClientFactory.create(
                properties.connectTimeout(),
                properties.readTimeout(),
                properties.transportSecurity()
        );
    }

    /**
     * 要求文本非空。 / Requires nonblank text.
     *
     * @param value     待校验文本 / text to validate
     * @param fieldName 用于错误消息的字段名 / field name used in error messages
     * @return 原始非空文本 / original nonblank text
     * @throws IllegalArgumentException 当文本为空时 / when the text is blank
     */
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    /**
     * 要求引用非空。 / Requires a nonnull reference.
     *
     * @param <T>       引用类型 / reference type
     * @param value     待校验引用 / reference to validate
     * @param fieldName 用于错误消息的字段名 / field name used in error messages
     * @return 原始非空引用 / original nonnull reference
     * @throws IllegalArgumentException 当引用为空时 / when the reference is null
     */
    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
