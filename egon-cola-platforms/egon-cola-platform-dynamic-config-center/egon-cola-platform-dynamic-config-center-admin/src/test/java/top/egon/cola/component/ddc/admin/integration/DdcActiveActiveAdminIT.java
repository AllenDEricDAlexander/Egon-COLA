package top.egon.cola.component.ddc.admin.integration;

import com.google.protobuf.Message;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.NameResolverRegistry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.egon.cola.component.ddc.admin.DynamicConfigCenterAdminApplication;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.InstanceStatus;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseExpiryScanner;
import top.egon.cola.component.ddc.admin.service.metadata.DdcBizService;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.admin.service.publish.PublishStartupRecovery;
import top.egon.cola.component.ddc.admin.service.publish.PublishTimeoutScanner;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcAckStatus;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.consumer.RpcDirectClientFactory;
import top.egon.cola.component.rpc.consumer.RpcDirectClientHandle;
import top.egon.cola.component.rpc.consumer.RpcDirectClientSettings;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;
import top.egon.cola.component.rpc.ddc.contract.DdcServiceRegistryRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRegistryProtoMapper;
import top.egon.cola.component.rpc.ddc.security.DdcRpcCanonicalRequest;
import top.egon.cola.component.rpc.ddc.security.DdcRpcClientInterceptorFactory;
import top.egon.cola.component.rpc.ddc.security.DdcRpcCredential;
import top.egon.cola.component.rpc.ddc.security.DdcRpcMetadataKeys;
import top.egon.cola.component.rpc.ddc.security.DdcRpcRequestSigner;
import top.egon.cola.component.rpc.provider.RpcProviderLifecycle;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DdcActiveActiveAdminIT {

    private static final String APP_CODE = "active-active-app";
    private static final String ACCESS_KEY = "active-active-access";
    private static final String SECRET = "active-active-secret";
    private static final String RESOURCE = "application.yml";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    private final List<AutoCloseable> clients = new ArrayList<>();
    private final ExecutorService publishExecutor =
            Executors.newVirtualThreadPerTaskExecutor();
    private ConfigurableApplicationContext nodeA;
    private ConfigurableApplicationContext nodeB;

    @BeforeAll
    void startTwoAdminNodes() {
        nodeA = node("a");
        nodeB = node("b");
        DdcAppEntity app = new DdcAppEntity();
        app.setId("app-active-active");
        app.setBizCode("default");
        app.setAppCode(APP_CODE);
        app.setAppName("Active Active Test");
        app.setEnabled(true);
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        nodeA.getBean(DdcAppRepository.class).saveAndFlush(app);
    }

    @AfterAll
    void stopTwoAdminNodes() throws Exception {
        for (AutoCloseable client : clients.reversed()) {
            client.close();
        }
        publishExecutor.close();
        if (nodeB != null) {
            nodeB.close();
        }
        if (nodeA != null) {
            nodeA.close();
        }
    }

    @Test
    @Order(1)
    void registryLeaseCanMoveFromNodeAToNodeBWithoutStickySession() {
        DdcCommonProtoMapper common = new DdcCommonProtoMapper(4 * 1024 * 1024);
        DdcRegistryProtoMapper mapper = new DdcRegistryProtoMapper(common);
        DdcServiceKey key = new DdcServiceKey(
                "default", "dev", APP_CODE,
                DdcServiceKind.RPC_PROVIDER,
                "active-active-service", "default", "1.0.0", "grpc");
        DdcServiceRegistration registration = new DdcServiceRegistration(
                "registry-instance", key, "127.0.0.1", 19091,
                false, Map.of(), 30, 5);

        DdcServiceRegistryRpc registryA = rpc(
                DdcServiceRegistryRpc.class, rpcPort(nodeA)).client();
        DdcServiceRegistryRpc registryB = rpc(
                DdcServiceRegistryRpc.class, rpcPort(nodeB)).client();
        var session = common.fromProto(registryA.registerService(
                mapper.toRegisterRequest(registration)).getSession());
        DdcServiceLeaseRequest lease = serviceLease(
                key, registration.instanceId(), session.leaseId());

        assertThat(common.fromProto(registryB.heartbeatService(
                        mapper.toHeartbeatRequest(lease)).getResult()).status())
                .isEqualTo(DdcLeaseOperationStatus.RENEWED);
        assertThat(common.fromProto(registryB.deregisterService(
                        mapper.toDeregisterRequest(lease)).getResult()).status())
                .isEqualTo(DdcLeaseOperationStatus.DELETED);
    }

    @Test
    @Order(2)
    void publishOnAAndAckOnBConvergesAndCrossNodePublicationIsSerialized()
            throws Exception {
        DdcConfigClient configA = configClient(nodeA);
        DdcConfigClient configB = configClient(nodeB);
        DdcManagementClient managementA = managementClient(nodeA);
        DdcManagementClient managementB = managementClient(nodeB);
        var lease = configA.register(configRegistration("config-instance", 30));

        DdcManagementConfig config = managementA.upsert(upsert(
                "feature:\n  value: one\n", null));
        Future<DdcManagementPublishResult> first = publishAsync(
                managementA, publish("change-cross-node", config.version(), config.content()));
        DdcPublishTaskEntity firstTask = awaitTask("change-cross-node");
        assertThat(configA.pull()).singleElement()
                .satisfies(value -> assertThat(value.getVersion())
                        .isEqualTo(firstTask.getTargetVersion()));
        acknowledge(configB, firstTask, DdcAckStatus.SUCCESS);
        assertThat(first.get(10, TimeUnit.SECONDS).status())
                .isEqualTo(DdcManagementPublishStatus.SUCCESS);

        config = managementA.upsert(upsert(
                "feature:\n  value: two\n", config.version()));
        DdcManagementPublishRequest same = publish(
                "change-idempotent", config.version(), config.content());
        Future<DdcManagementPublishResult> sameA = publishAsync(managementA, same);
        Future<DdcManagementPublishResult> sameB = publishAsync(managementB, same);
        DdcPublishTaskEntity sameTask = awaitTask(same.changeId());
        acknowledge(configB, sameTask, DdcAckStatus.SUCCESS);
        assertThat(sameA.get(10, TimeUnit.SECONDS).status()).isEqualTo(
                DdcManagementPublishStatus.SUCCESS);
        assertThat(sameB.get(10, TimeUnit.SECONDS).status()).isEqualTo(
                DdcManagementPublishStatus.SUCCESS);
        assertThat(acks(same.changeId())).hasSize(1);

        config = managementA.upsert(upsert(
                "feature:\n  value: three\n", config.version()));
        Future<DdcManagementPublishResult> differentA = publishAsync(
                managementA, publish("change-different-a", config.version(), config.content()));
        Future<DdcManagementPublishResult> differentB = publishAsync(
                managementB, publish("change-different-b", config.version(), config.content()));
        DdcPublishTaskEntity active = awaitOneTask(
                "change-different-a", "change-different-b");
        acknowledge(configB, active, DdcAckStatus.SUCCESS);

        List<Object> publicationOutcomes = outcomes(differentA, differentB);
        int successes = publicationOutcomes.stream()
                .mapToInt(outcome -> outcome instanceof DdcManagementPublishResult ? 1 : 0)
                .sum();
        assertThat(successes).isEqualTo(1);
        assertThat(publicationOutcomes.stream()
                .filter(DdcManagementClientException.class::isInstance))
                .hasSize(1);

        DdcHeartbeatRequest offline = heartbeat(
                "config-instance", lease.leaseId());
        assertThat(configB.offline(offline).status())
                .isEqualTo(DdcLeaseOperationStatus.DELETED);
    }

    @Test
    @Order(3)
    void conflictingAckAndConcurrentSchedulersProduceOneStoredOutcome()
            throws Exception {
        DdcConfigClient configA = configClient(nodeA);
        DdcConfigClient configB = configClient(nodeB);
        DdcManagementClient management = managementClient(nodeA);
        configA.register(configRegistration("scheduler-instance", 5));

        DdcManagementConfig config = management.upsert(upsert(
                "feature:\n  value: scheduler\n", 3L));
        Future<DdcManagementPublishResult> conflict = publishAsync(
                management,
                publish("change-conflicting-ack", config.version(), config.content()));
        DdcPublishTaskEntity conflictTask = awaitTask("change-conflicting-ack");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> success = executor.submit(() ->
                    acknowledge(configA, conflictTask, DdcAckStatus.SUCCESS));
            Future<?> failed = executor.submit(() ->
                    acknowledge(configB, conflictTask, DdcAckStatus.FAILED));
            success.get(10, TimeUnit.SECONDS);
            failed.get(10, TimeUnit.SECONDS);
        }
        DdcPublishTaskEntity stored = task("change-conflicting-ack");
        DdcPublishAckEntity storedAck = acks("change-conflicting-ack").getFirst();
        assertThat(storedAck.getAckStatus()).isIn("SUCCESS", "FAILED");
        assertThat(stored.getStatus()).isEqualTo(storedAck.getAckStatus());
        assertThat(conflict.get(10, TimeUnit.SECONDS).status().name())
                .isEqualTo(stored.getStatus());

        DdcManagementConfig timeoutConfig = management.upsert(upsert(
                "feature:\n  value: timeout\n", config.version()));
        Future<DdcManagementPublishResult> timeout = publishAsync(
                management,
                publish("change-timeout-scan", timeoutConfig.version(), timeoutConfig.content()));
        DdcPublishTaskEntity timeoutTask = awaitTask("change-timeout-scan");
        timeoutTask.setDispatchedAt(LocalDateTime.now().minusMinutes(1));
        timeoutTask.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        timeoutTask.setTimeoutMs(1L);
        tasks().saveAndFlush(timeoutTask);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> scanA = executor.submit(() ->
                    nodeA.getBean(PublishTimeoutScanner.class).scanExpired());
            Future<Integer> scanB = executor.submit(() ->
                    nodeB.getBean(PublishTimeoutScanner.class).scanExpired());
            scanA.get(10, TimeUnit.SECONDS);
            scanB.get(10, TimeUnit.SECONDS);
        }
        assertThat(timeout.get(10, TimeUnit.SECONDS).status())
                .isEqualTo(DdcManagementPublishStatus.TIMEOUT);
        assertThat(acks("change-timeout-scan"))
                .singleElement()
                .extracting(DdcPublishAckEntity::getAckStatus)
                .isEqualTo(DdcAckStatus.TIMEOUT.name());

        DdcPublishTaskEntity stale = task("change-timeout-scan");
        stale.setStatus(PublishStatus.UNKNOWN.name());
        stale.setCompletedAt(null);
        stale.setUpdatedAt(LocalDateTime.now().minusHours(2));
        tasks().saveAndFlush(stale);
        DdcPublishAckEntity staleTarget = acks(stale.getChangeId()).getFirst();
        staleTarget.setCurrentVersion(null);
        staleTarget.setAckStatus(null);
        staleTarget.setErrorMessage(null);
        staleTarget.setAckAt(null);
        ackRepository().saveAndFlush(staleTarget);
        int recovered;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> recoveryA = executor.submit(() ->
                    nodeA.getBean(PublishStartupRecovery.class).recoverStale());
            Future<Integer> recoveryB = executor.submit(() ->
                    nodeB.getBean(PublishStartupRecovery.class).recoverStale());
            recovered = recoveryA.get(10, TimeUnit.SECONDS)
                    + recoveryB.get(10, TimeUnit.SECONDS);
        }
        assertThat(recovered).isEqualTo(1);
        assertThat(task(stale.getChangeId()).getStatus())
                .isEqualTo(PublishStatus.PUBLISHING.name());
        assertThat(acks(stale.getChangeId())).hasSize(1);
        acknowledge(configB, task(stale.getChangeId()), DdcAckStatus.SUCCESS);

        await().atMost(java.time.Duration.ofSeconds(8)).untilAsserted(() -> {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<Integer> leaseA = executor.submit(() ->
                        nodeA.getBean(DdcLeaseExpiryScanner.class).scanExpired());
                Future<Integer> leaseB = executor.submit(() ->
                        nodeB.getBean(DdcLeaseExpiryScanner.class).scanExpired());
                leaseA.get(5, TimeUnit.SECONDS);
                leaseB.get(5, TimeUnit.SECONDS);
            }
            DdcInstanceEntity instance = nodeB.getBean(DdcInstanceRepository.class)
                    .findByInstanceId("scheduler-instance").orElseThrow();
            assertThat(instance.getStatus()).isEqualTo(InstanceStatus.OFFLINE.name());
        });
    }

    @Test
    @Order(4)
    void nonceIsSharedAndScopeCacheExpiresAcrossNodes() throws Exception {
        PullConfigRequest request = PullConfigRequest.newBuilder()
                .setScope(top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope
                        .newBuilder()
                        .setBizCode("default")
                        .setEnv("dev")
                        .setAppCode(APP_CODE))
                .build();
        String method = DdcConfigRuntimeServiceGrpc.getPullConfigMethod()
                .getFullMethodName();
        Metadata signed = signed(method, request, "shared-replay-nonce");
        try (GrpcChannel first = grpc(nodeA); GrpcChannel second = grpc(nodeB)) {
            DdcConfigRuntimeServiceGrpc.newBlockingStub(first.channel())
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(signed))
                    .pullConfig(request);
            assertThatThrownBy(() -> DdcConfigRuntimeServiceGrpc
                    .newBlockingStub(second.channel())
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(signed))
                    .pullConfig(request))
                    .isInstanceOfSatisfying(StatusRuntimeException.class,
                            failure -> assertThat(failure.getStatus().getCode())
                                    .isEqualTo(Status.Code.UNAUTHENTICATED));
        }

        DdcScopeGate scopeB = nodeB.getBean(DdcScopeGate.class);
        scopeB.assertPhysicalEnabled("default", APP_CODE, "dev");
        nodeA.getBean(DdcBizService.class).setEnabled("default", false);
        assertThatCode(() -> scopeB.assertPhysicalEnabled(
                "default", APP_CODE, "dev")).doesNotThrowAnyException();
        await().atMost(java.time.Duration.ofSeconds(7)).untilAsserted(() ->
                assertThatThrownBy(() -> scopeB.assertPhysicalEnabled(
                        "default", APP_CODE, "dev")).isInstanceOf(RuntimeException.class));
        nodeA.getBean(DdcBizService.class).setEnabled("default", true);
        scopeB.invalidate("biz:default");
    }

    @Test
    @Order(5)
    void oneLogicalDirectChannelFailsOverAfterNodeAStops() {
        StaticAddressResolver resolver = new StaticAddressResolver(
                rpcPort(nodeA), rpcPort(nodeB));
        NameResolverRegistry.getDefaultRegistry().register(resolver);
        try {
            DdcRpcClientHandle<DdcConfigClient> handle =
                    ddcClientFactory("active-active:///ddc").configClient();
            clients.add(handle);
            assertThat(handle.client().pull()).isNotNull();
            nodeA.close();
            nodeA = null;
            await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(handle.client().pull()).isNotNull());
        } finally {
            NameResolverRegistry.getDefaultRegistry().deregister(resolver);
        }
    }

    private ConfigurableApplicationContext node(String id) {
        return new SpringApplicationBuilder(DynamicConfigCenterAdminApplication.class)
                .profiles("active-active")
                .run(nodeArguments(id));
    }

    private String[] nodeArguments(String id) {
        return new String[]{
                "--spring.application.name=ddc-active-active-" + id,
                "--server.port=0",
                "--spring.jmx.enabled=false",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.flyway.enabled=true",
                "--egon.cola.platform.idp.enabled=false",
                "--egon.cola.platform.rbac3.enabled=false",
                "--egon.cola.component.ddc.enabled=false",
                "--egon.cola.component.ddc.registry.enabled=false",
                "--egon.cola.component.ddc.admin.security.local-dev=true",
                "--egon.cola.component.ddc.admin.redis.host=" + REDIS.getHost(),
                "--egon.cola.component.ddc.admin.redis.port=" + REDIS.getMappedPort(6379),
                "--egon.cola.component.ddc.admin.rpc.signature-enabled=true",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].credential-id=active-active",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].access-key=" + ACCESS_KEY,
                "--egon.cola.component.ddc.admin.rpc.credentials[0].secret=" + SECRET,
                "--egon.cola.component.ddc.admin.rpc.credentials[0].client-type=*",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].app-code-patterns[0]=*",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].env-patterns[0]=*",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].biz-code-patterns[0]=*",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].allowed-operations[0]=*",
                "--egon.cola.component.ddc.admin.publish.scan-interval-ms=3600000",
                "--egon.cola.component.ddc.admin.publish.recovery-stale-ms=3600000",
                "--egon.cola.component.ddc.admin.lease.scan-interval-millis=3600000",
                "--egon.cola.component.rpc.enabled=true",
                "--egon.cola.component.rpc.provider.enabled=true",
                "--egon.cola.component.rpc.provider.port=0",
                "--egon.cola.component.rpc.provider.registration-mode=DISABLED",
                "--egon.cola.component.rpc.consumer.enabled=false",
                "--egon.cola.component.rpc.tls.development-plaintext=true"
        };
    }

    private int rpcPort(ConfigurableApplicationContext context) {
        return context.getBean(RpcProviderLifecycle.class).boundPort();
    }

    private DdcConfigClient configClient(ConfigurableApplicationContext context) {
        DdcRpcClientHandle<DdcConfigClient> handle =
                ddcClientFactory(target(context)).configClient();
        clients.add(handle);
        return handle.client();
    }

    private DdcManagementClient managementClient(
            ConfigurableApplicationContext context) {
        DdcRpcClientHandle<DdcManagementClient> handle =
                ddcClientFactory(target(context)).managementClient();
        clients.add(handle);
        return handle.client();
    }

    private DdcRpcClientFactory ddcClientFactory(String target) {
        DdcRpcProperties rpc = new DdcRpcProperties();
        rpc.setTarget(target);
        rpc.getTls().setDevelopmentPlaintext(true);
        credential(rpc.getAuth().getRuntime());
        credential(rpc.getAuth().getRegistry());
        credential(rpc.getAuth().getManagement());
        DdcProperties ddc = new DdcProperties();
        ddc.setBizCode("default");
        ddc.setEnv("dev");
        ddc.setAppCode(APP_CODE);
        return new DdcRpcClientFactory(
                rpc,
                ddc,
                new RpcProcessIdentity(
                        "active-active-it", "test", "127.0.0.1", 1L, "it-client")
        );
    }

    private void credential(DdcRpcProperties.Credential credential) {
        credential.setAccessKey(ACCESS_KEY);
        credential.setSecretKey(SECRET);
    }

    private <T> RpcDirectClientHandle<T> rpc(Class<T> contract, int port) {
        RpcDirectClientHandle<T> handle = new RpcDirectClientFactory().create(
                contract,
                RpcDirectClientSettings.defaults(
                        "dns:///127.0.0.1:" + port,
                        new RpcProcessIdentity(
                                "active-active-it", "test", "127.0.0.1", 1L, "raw-it"),
                        new RpcTransportSecurity(false, true, null, null, null),
                        10000
                ),
                List.of(new DdcRpcClientInterceptorFactory(
                        new DdcRpcCredential(ACCESS_KEY, SECRET)))
        );
        clients.add(handle);
        return handle;
    }

    private String target(ConfigurableApplicationContext context) {
        return "dns:///127.0.0.1:" + rpcPort(context);
    }

    private DdcInstanceRegisterRequest configRegistration(
            String instanceId,
            int leaseSeconds) {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        request.setInstanceId(instanceId);
        request.setBizCode("default");
        request.setEnv("dev");
        request.setAppCode(APP_CODE);
        request.setHost("127.0.0.1");
        request.setPort(19100);
        request.setPid("1");
        request.setSdkVersion("it");
        request.setLeaseSeconds(leaseSeconds);
        request.setHeartbeatIntervalSeconds(1);
        return request;
    }

    private DdcHeartbeatRequest heartbeat(String instanceId, String leaseId) {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        request.setInstanceId(instanceId);
        request.setLeaseId(leaseId);
        request.setBizCode("default");
        request.setEnv("dev");
        request.setAppCode(APP_CODE);
        request.setHost("127.0.0.1");
        request.setPort(19100);
        request.setPid("1");
        request.setSdkVersion("it");
        return request;
    }

    private DdcServiceLeaseRequest serviceLease(
            DdcServiceKey key,
            String instanceId,
            String leaseId) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(key);
        request.setInstanceId(instanceId);
        request.setLeaseId(leaseId);
        return request;
    }

    private DdcManagementConfigUpsertRequest upsert(
            String content,
            Long expectedVersion) {
        return new DdcManagementConfigUpsertRequest(
                "default", "dev", APP_CODE, RESOURCE,
                content, "YAML", "active-active integration",
                expectedVersion, "test-requested-operator");
    }

    private DdcManagementPublishRequest publish(
            String changeId,
            Long expectedVersion,
            String content) {
        return new DdcManagementPublishRequest(
                "default", "dev", APP_CODE, RESOURCE,
                content, "YAML", expectedVersion, changeId,
                15000L, "test-requested-operator");
    }

    private Future<DdcManagementPublishResult> publishAsync(
            DdcManagementClient client,
            DdcManagementPublishRequest request) {
        return publishExecutor.submit(() -> client.publish(request));
    }

    private DdcPublishTaskEntity awaitTask(String changeId) {
        await().atMost(java.time.Duration.ofSeconds(10)).until(() ->
                tasks().findByChangeId(changeId).isPresent()
                        && !acks(changeId).isEmpty());
        return task(changeId);
    }

    private DdcPublishTaskEntity awaitOneTask(String first, String second) {
        await().atMost(java.time.Duration.ofSeconds(10)).until(() ->
                tasks().findByChangeId(first).isPresent()
                        || tasks().findByChangeId(second).isPresent());
        return tasks().findByChangeId(first)
                .or(() -> tasks().findByChangeId(second))
                .orElseThrow();
    }

    private void acknowledge(
            DdcConfigClient client,
            DdcPublishTaskEntity task,
            DdcAckStatus status) {
        DdcPublishAckEntity target = acks(task.getChangeId()).getFirst();
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(task.getChangeId());
        request.setBizCode(task.getBizCode());
        request.setEnv(task.getEnv());
        request.setAppCode(task.getAppCode());
        request.setInstanceId(target.getInstanceId());
        request.setLeaseId(target.getLeaseId());
        request.setResourceName(task.getResourceName());
        request.setTargetVersion(task.getTargetVersion());
        request.setCurrentVersion(task.getTargetVersion());
        request.setResourceChecksum(task.getResourceChecksum());
        request.setStatus(status);
        request.setAckTime(System.currentTimeMillis());
        client.ack(request);
    }

    private List<Object> outcomes(
            Future<DdcManagementPublishResult> first,
            Future<DdcManagementPublishResult> second) throws Exception {
        List<Object> outcomes = new ArrayList<>();
        outcomes.add(outcome(first));
        outcomes.add(outcome(second));
        return outcomes;
    }

    private Object outcome(Future<DdcManagementPublishResult> future)
            throws Exception {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException failure) {
            return failure.getCause();
        }
    }

    private DdcPublishTaskEntity task(String changeId) {
        return tasks().findByChangeId(changeId).orElseThrow();
    }

    private DdcPublishTaskRepository tasks() {
        return nodeB.getBean(DdcPublishTaskRepository.class);
    }

    private DdcPublishAckRepository ackRepository() {
        return nodeB.getBean(DdcPublishAckRepository.class);
    }

    private List<DdcPublishAckEntity> acks(String changeId) {
        return ackRepository().findByChangeId(changeId);
    }

    private Metadata signed(String method, Message request, String nonce) {
        long timestamp = System.currentTimeMillis();
        DdcRpcCanonicalRequest canonical = new DdcRpcCanonicalRequest(
                method, timestamp, nonce, request);
        Metadata metadata = new Metadata();
        metadata.put(DdcRpcMetadataKeys.ACCESS_KEY, ACCESS_KEY);
        metadata.put(DdcRpcMetadataKeys.TIMESTAMP, Long.toString(timestamp));
        metadata.put(DdcRpcMetadataKeys.NONCE, nonce);
        metadata.put(DdcRpcMetadataKeys.CONTENT_SHA256, canonical.contentSha256());
        metadata.put(
                DdcRpcMetadataKeys.SIGNATURE,
                new DdcRpcRequestSigner().sign(canonical, SECRET));
        metadata.put(
                DdcRpcMetadataKeys.CONTRACT_VERSION,
                DdcRpcCanonicalRequest.CONTRACT_VERSION);
        return metadata;
    }

    private GrpcChannel grpc(ConfigurableApplicationContext context) {
        return new GrpcChannel(NettyChannelBuilder
                .forAddress("127.0.0.1", rpcPort(context))
                .usePlaintext()
                .build());
    }

    private record GrpcChannel(ManagedChannel channel) implements AutoCloseable {
        @Override
        public void close() throws InterruptedException {
            channel.shutdownNow();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static final class StaticAddressResolver
            extends NameResolverProvider {

        private final List<EquivalentAddressGroup> addresses;

        private StaticAddressResolver(int firstPort, int secondPort) {
            addresses = List.of(
                    new EquivalentAddressGroup(
                            new InetSocketAddress("127.0.0.1", firstPort)),
                    new EquivalentAddressGroup(
                            new InetSocketAddress("127.0.0.1", secondPort))
            );
        }

        @Override
        protected boolean isAvailable() {
            return true;
        }

        @Override
        protected int priority() {
            return 10;
        }

        @Override
        public String getDefaultScheme() {
            return "active-active";
        }

        @Override
        public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
            if (!getDefaultScheme().equals(targetUri.getScheme())) {
                return null;
            }
            return new NameResolver() {
                @Override
                public String getServiceAuthority() {
                    return "ddc-active-active";
                }

                @Override
                public void start(Listener2 listener) {
                    listener.onResult(ResolutionResult.newBuilder()
                            .setAddresses(addresses)
                            .setAttributes(Attributes.EMPTY)
                            .build());
                }

                @Override
                public void shutdown() {
                }
            };
        }
    }
}
