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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DdcOpenApiServiceRegistryClient
        implements DdcServiceRegistryClient, AutoCloseable {

    private static final String REGISTER_PATH =
            "/api/v1/ddc/openapi/registry/instances/register";

    private static final String HEARTBEAT_PATH =
            "/api/v1/ddc/openapi/registry/instances/heartbeat";

    private static final String DEREGISTER_PATH =
            "/api/v1/ddc/openapi/registry/instances/deregister";

    private static final String INSTANCES_PATH =
            "/api/v1/ddc/openapi/registry/instances";

    private static final String SERVICES_PATH =
            "/api/v1/ddc/openapi/registry/services";

    private final DdcProperties properties;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final DdcRequestSigner signer = new DdcRequestSigner();

    private final DdcRegistrySubscriptionManager subscriptionManager;

    private final Map<String, ActiveRegistration> registrations = new ConcurrentHashMap<>();

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
        registrations.put(
                registration.instanceId(),
                new ActiveRegistration(registration.serviceKey(), session)
        );
        return session;
    }

    @Override
    public DdcLeaseOperationResult heartbeat(String instanceId, String leaseId) {
        ActiveRegistration registration = active(instanceId, leaseId);
        DdcLeaseOperationResult result = post(
                HEARTBEAT_PATH,
                leaseRequest(instanceId, leaseId, registration.serviceKey()),
                new ParameterizedTypeReference<>() {
                }
        );
        if (result.status() == DdcLeaseOperationStatus.NOT_FOUND
                || result.status() == DdcLeaseOperationStatus.LEASE_MISMATCH) {
            registrations.remove(instanceId, registration);
        }
        return result;
    }

    @Override
    public DdcLeaseOperationResult deregister(String instanceId, String leaseId) {
        ActiveRegistration registration = active(instanceId, leaseId);
        DdcLeaseOperationResult result = post(
                DEREGISTER_PATH,
                leaseRequest(instanceId, leaseId, registration.serviceKey()),
                new ParameterizedTypeReference<>() {
                }
        );
        if (result.status() != DdcLeaseOperationStatus.NOT_DELETED) {
            registrations.remove(instanceId, registration);
        }
        return result;
    }

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

    @Override
    public DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener) {
        return subscriptionManager.subscribe(serviceKey, listener);
    }

    @Override
    public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        parameters.put("env", List.of(query.env()));
        parameters.put("namespace", List.of(query.namespace()));
        parameters.put("serviceKind", List.of(query.serviceKind().name()));
        parameters.put("protocol", List.of(query.protocol()));
        putIfPresent(parameters, "serviceName", query.serviceName());
        putIfPresent(parameters, "group", query.group());
        putIfPresent(parameters, "version", query.version());
        return get(
                SERVICES_PATH,
                parameters,
                new ParameterizedTypeReference<>() {
                }
        );
    }

    @Override
    public DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener) {
        return subscriptionManager.subscribeServices(query, listener);
    }

    @Override
    public void close() {
        subscriptionManager.close();
        registrations.clear();
    }

    private ActiveRegistration active(String instanceId, String leaseId) {
        ActiveRegistration registration = registrations.get(instanceId);
        if (registration == null || !registration.session().leaseId().equals(leaseId)) {
            throw new DdcException(DdcErrorStatus.LEASE_MISMATCH);
        }
        return registration;
    }

    private DdcServiceLeaseRequest leaseRequest(String instanceId,
                                                String leaseId,
                                                DdcServiceKey serviceKey) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setEnv(serviceKey.env());
        request.setNamespace(serviceKey.namespace());
        request.setServiceKind(serviceKey.serviceKind());
        request.setServiceKey(serviceKey);
        request.setInstanceId(instanceId);
        request.setLeaseId(leaseId);
        return request;
    }

    private Map<String, List<String>> serviceKeyQuery(DdcServiceKey serviceKey) {
        Map<String, List<String>> query = new LinkedHashMap<>();
        query.put("env", List.of(serviceKey.env()));
        query.put("namespace", List.of(serviceKey.namespace()));
        query.put("serviceKind", List.of(serviceKey.serviceKind().name()));
        query.put("serviceName", List.of(serviceKey.serviceName()));
        query.put("group", List.of(serviceKey.group()));
        query.put("version", List.of(serviceKey.version()));
        query.put("protocol", List.of(serviceKey.protocol()));
        return query;
    }

    private void putIfPresent(Map<String, List<String>> target,
                              String name,
                              String value) {
        if (value != null && !value.isBlank()) {
            target.put(name, List.of(value));
        }
    }

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

    private byte[] serialize(Object request) {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE, exception);
        }
    }

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

    private void prepareHeaders(HttpHeaders headers, DdcCanonicalRequest request) {
        DdcTraceSupport.inject(headers);
        sign(headers, request);
    }

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

    private record ActiveRegistration(
            DdcServiceKey serviceKey,
            DdcLeaseSession session
    ) {
    }
}
