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
import top.egon.cola.component.common.pojo.ResultRecord;
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
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class HttpDdcManagementClient implements DdcManagementClient {

    private static final String MANAGEMENT_PATH = "/api/v1/ddc/openapi/management";

    private final RestClient restClient;

    private final DdcManagementRequestFactory requestFactory;

    public HttpDdcManagementClient(DdcManagementClientProperties properties) {
        this(
                properties,
                defaultBuilder(properties),
                Clock.systemUTC(),
                () -> UUID.randomUUID().toString()
        );
    }

    HttpDdcManagementClient(
            DdcManagementClientProperties properties,
            RestClient.Builder restClientBuilder,
            Clock clock,
            Supplier<String> nonceSupplier
    ) {
        require(properties, "properties");
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.requestFactory = new DdcManagementRequestFactory(
                properties,
                objectMapper,
                require(clock, "clock"),
                require(nonceSupplier, "nonceSupplier")
        );
        this.restClient = require(restClientBuilder, "restClientBuilder")
                .baseUrl(properties.endpoint())
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }

    @Override
    public Optional<DdcManagementConfig> findConfig(DdcManagementConfigQuery query) {
        require(query, "query");
        try {
            return Optional.of(exchange(
                    HttpMethod.GET,
                    configPath(
                            query.appCode(),
                            query.env(),
                            query.namespace(),
                            query.configKey()
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

    @Override
    public DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request) {
        require(request, "request");
        return exchange(
                HttpMethod.PUT,
                configPath(
                        request.appCode(),
                        request.env(),
                        request.namespace(),
                        request.configKey()
                ),
                Map.of(),
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public void delete(DdcManagementConfigDeleteRequest request) {
        require(request, "request");
        exchange(
                HttpMethod.DELETE,
                configPath(
                        request.appCode(),
                        request.env(),
                        request.namespace(),
                        request.configKey()
                ),
                Map.of(),
                request,
                new ParameterizedTypeReference<ResultRecord<Void>>() {
                },
                false
        );
    }

    @Override
    public DdcManagementPublishResult publish(DdcManagementPublishRequest request) {
        require(request, "request");
        return exchange(
                HttpMethod.POST,
                configPath(
                        request.appCode(),
                        request.env(),
                        request.namespace(),
                        request.configKey()
                ) + "/publish",
                Map.of(),
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

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

    private <T> T exchange(
            HttpMethod method,
            String path,
            Map<String, List<String>> query,
            Object request,
            ParameterizedTypeReference<ResultRecord<T>> responseType,
            boolean required
    ) {
        DdcManagementRequestFactory.SignedRequest signedRequest =
                requestFactory.create(method, path, query, request);
        try {
            RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(signedRequest.target())
                    .headers(headers -> headers.addAll(signedRequest.headers()));
            if (signedRequest.hasBody()) {
                spec.contentType(MediaType.APPLICATION_JSON).body(signedRequest.body());
            }
            ResultRecord<T> result = spec.retrieve().body(responseType);
            return data(result, required);
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

    private String configPath(
            String appCode,
            String env,
            String namespace,
            String configKey
    ) {
        return MANAGEMENT_PATH
                + "/configs/"
                + segment(appCode, "appCode") + "/"
                + segment(env, "env") + "/"
                + segment(namespace, "namespace") + "/"
                + segment(configKey, "configKey");
    }

    private Map<String, List<String>> configClientQuery(DdcManagementInstanceQuery query) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        putQuery(values, "appCode", query.appCode());
        putQuery(values, "env", query.env());
        putQuery(values, "namespace", query.namespace());
        return values;
    }

    private Map<String, List<String>> serviceQuery(DdcManagementServiceQuery query) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        putQuery(values, "env", query.env());
        putQuery(values, "namespace", query.namespace());
        putQuery(values, "serviceKind", query.serviceKind());
        putQuery(values, "protocol", query.protocol());
        putQuery(values, "serviceName", query.serviceName());
        putQuery(values, "group", query.group());
        putQuery(values, "version", query.version());
        return values;
    }

    private void putQuery(Map<String, List<String>> query, String name, String value) {
        if (value != null && !value.isBlank()) {
            query.put(name, List.of(value));
        }
    }

    private String segment(String value, String fieldName) {
        return UriUtils.encodePathSegment(
                requireText(value, fieldName),
                StandardCharsets.UTF_8
        );
    }

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

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
