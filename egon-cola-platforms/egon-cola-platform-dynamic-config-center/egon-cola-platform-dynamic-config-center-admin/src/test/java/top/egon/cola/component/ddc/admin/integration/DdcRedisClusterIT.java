package top.egon.cola.component.ddc.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.redisson.Redisson;
import org.redisson.api.HostPortNatMapper;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.config.DdcRedisTopology;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DdcRedisClusterIT {

    private static final String REDIS_IMAGE = "redis:7.4-alpine";

    private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

    private final Network network = Network.newNetwork();

    private final List<GenericContainer<?>> nodes = new ArrayList<>();

    private RedissonClient redisson;

    @BeforeAll
    void startTopology() throws Exception {
        for (int index = 1; index <= 3; index++) {
            GenericContainer<?> node = node(index);
            node.start();
            nodes.add(node);
        }
        Container.ExecResult create = nodes.getFirst().execInContainer(
                "redis-cli",
                "--cluster", "create",
                "redis-cluster-1:6379",
                "redis-cluster-2:6379",
                "redis-cluster-3:6379",
                "--cluster-replicas", "0",
                "--cluster-yes"
        );
        assertThat(create.getExitCode()).isZero();
        assertThat(create.getStdout()).contains("[OK] All 16384 slots covered");

        Config config = DdcRedisTopology.create(
                "CLUSTER",
                nodes.stream().map(this::address).toList(),
                null,
                null,
                0,
                null,
                0
        );
        config.useClusterServers()
                .setScanInterval(500)
                .setNatMapper(natMapper());
        redisson = Redisson.create(config);
    }

    @AfterAll
    void stopTopology() {
        if (redisson != null) {
            redisson.shutdown();
        }
        nodes.reversed().forEach(GenericContainer::stop);
        network.close();
    }

    @Test
    void configAndLeaseLifecycleCompletesWithoutCrossSlot() {
        DdcRedisRepository configs = new DdcRedisRepository(redisson);
        DdcConfigLeaseRedisRepository leases = new DdcConfigLeaseRedisRepository(
                redisson,
                new ObjectMapper()
        );
        DdcInstanceIdentity identity = new DdcInstanceIdentity(
                "config-client-1", "demo", "test", "default",
                "127.0.0.1", 8080, "100", "5.2.3"
        );
        DdcLeaseSession session = new DdcLeaseSession(
                identity.instanceId(), "lease-1", DdcLeaseRole.CONFIG_CLIENT,
                1, 1, NOW, NOW.plusSeconds(1)
        );

        configs.writeConfig("demo", "test", "default", "switch", "on", 1L);
        configs.publish(message());
        assertThat(configs.readConfigValue(
                "demo", "test", "default", "switch"
        )).isEqualTo("on");
        assertThat(configs.readConfigVersion(
                "demo", "test", "default", "switch"
        )).isEqualTo(1L);

        leases.register(identity, session, NOW);
        assertThat(leases.activeTargets(
                "demo", "test", "default", NOW
        )).hasSize(1);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(leases.removeExpiredProjection(
                        "demo", "test", "default",
                        identity.instanceId(), session.leaseId(), NOW.plusSeconds(2)
                )).isTrue()
        );
        assertThat(leases.activeTargets(
                "demo", "test", "default", NOW.plusSeconds(2)
        )).isEmpty();
    }

    @Test
    void serviceRegistrationHeartbeatAndDeregistrationCompleteWithoutCrossSlot() {
        DdcServiceRegistryRedisRepository registry =
                new DdcServiceRegistryRedisRepository(
                        redisson,
                        new ObjectMapper().registerModule(new JavaTimeModule())
                );
        DdcServiceInstance instance = instance();

        registry.register(instance);
        assertThat(registry.getInstances(instance.serviceKey(), NOW).instances())
                .extracting(DdcServiceInstance::instanceId)
                .containsExactly(instance.instanceId());
        assertThat(registry.heartbeat(serviceLease(instance), NOW.plusSeconds(1)).status())
                .isEqualTo(DdcLeaseOperationStatus.RENEWED);
        assertThat(registry.deregister(serviceLease(instance), NOW.plusSeconds(2)).status())
                .isEqualTo(DdcLeaseOperationStatus.DELETED);
        assertThat(registry.getInstances(instance.serviceKey(), NOW.plusSeconds(2)).instances())
                .isEmpty();
    }

    private GenericContainer<?> node(int index) {
        return new GenericContainer<>(REDIS_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("redis-cluster-" + index)
                .withExposedPorts(6379)
                .withCommand(
                        "redis-server",
                        "--cluster-enabled", "yes",
                        "--cluster-config-file", "nodes.conf",
                        "--cluster-node-timeout", "5000",
                        "--appendonly", "no",
                        "--protected-mode", "no"
                )
                .waitingFor(Wait.forListeningPort());
    }

    private String address(GenericContainer<?> node) {
        return "redis://" + node.getHost() + ":" + node.getMappedPort(6379);
    }

    private HostPortNatMapper natMapper() {
        Map<String, String> mappings = new LinkedHashMap<>();
        for (GenericContainer<?> node : nodes) {
            String internal = containerIp(node) + ":6379";
            String mapped = node.getHost() + ":" + node.getMappedPort(6379);
            mappings.put(internal, mapped);
        }
        HostPortNatMapper mapper = new HostPortNatMapper();
        mapper.setHostsPortMap(mappings);
        return mapper;
    }

    private String containerIp(GenericContainer<?> container) {
        return container.getContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .values()
                .iterator()
                .next()
                .getIpAddress();
    }

    private DdcPublishMessage message() {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("change-1");
        message.setAppCode("demo");
        message.setEnv("test");
        message.setNamespace("default");
        message.setConfigKey("switch");
        message.setConfigValue("on");
        message.setTargetVersion(1L);
        return message;
    }

    private DdcServiceInstance instance() {
        DdcServiceKey serviceKey = new DdcServiceKey(
                "pay-biz",
                "orders-app",
                "test",
                "default",
                DdcServiceKind.RPC_PROVIDER,
                "order.v1.OrderQueryService",
                "default",
                "1.0.0",
                "grpc"
        );
        return new DdcServiceInstance(
                "provider-1", "service-lease-1", serviceKey,
                "127.0.0.1", 19090, false, Map.of("zone", "east"),
                30, 10, NOW, NOW, NOW.plusSeconds(30), "ONLINE", 0L
        );
    }

    private DdcServiceLeaseRequest serviceLease(DdcServiceInstance instance) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(instance.serviceKey());
        request.setInstanceId(instance.instanceId());
        request.setLeaseId(instance.leaseId());
        return request;
    }
}
