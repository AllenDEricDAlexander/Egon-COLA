package top.egon.cola.component.ddc.admin.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.redisson.Redisson;
import org.redisson.api.HostPortNatMapper;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.redis.DdcRedisClientFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DdcRedisSentinelIT {

    private static final String REDIS_IMAGE = "redis:7.4-alpine";

    private static final String MASTER_NAME = "ddc-master";

    private static final String MASTER_ALIAS = "redis-master";

    private static final String REPLICA_ALIAS = "redis-replica";

    private final Network network = Network.newNetwork();

    private final GenericContainer<?> master = redis(MASTER_ALIAS);

    private final GenericContainer<?> replica = new GenericContainer<>(REDIS_IMAGE)
            .withNetwork(network)
            .withNetworkAliases(REPLICA_ALIAS)
            .withExposedPorts(6379)
            .withCommand(
                    "redis-server",
                    "--replicaof", MASTER_ALIAS, "6379",
                    "--replica-announce-ip", REPLICA_ALIAS,
                    "--replica-announce-port", "6379",
                    "--appendonly", "no",
                    "--protected-mode", "no"
            )
            .waitingFor(Wait.forListeningPort());

    private final List<GenericContainer<?>> sentinels = new ArrayList<>();

    private RedissonClient redisson;

    @BeforeAll
    void startTopology() {
        master.start();
        replica.start();
        for (int index = 1; index <= 3; index++) {
            GenericContainer<?> sentinel = sentinel(index);
            sentinel.start();
            sentinels.add(sentinel);
        }

        Config config = DdcRedisClientFactory.configuration(
                "SENTINEL",
                sentinels.stream().map(this::address).toList(),
                MASTER_NAME,
                null,
                0,
                null,
                0
        );
        config.useSentinelServers()
                .setReadMode(ReadMode.MASTER)
                .setScanInterval(500)
                .setCheckSentinelsList(false)
                .setSentinelsDiscovery(false)
                .setNatMapper(natMapper());
        redisson = Redisson.create(config);
    }

    @AfterAll
    void stopTopology() {
        if (redisson != null) {
            redisson.shutdown();
        }
        sentinels.reversed().forEach(GenericContainer::stop);
        replica.stop();
        master.stop();
        network.close();
    }

    @Test
    void reconnectsAfterSentinelPromotesTheReplica() throws Exception {
        DdcRedisRepository repository = new DdcRedisRepository(redisson);
        repository.writeConfig(
                "sentinel-demo", "test", "default", "switch", "before", 1L
        );
        assertThat(master.execInContainer("redis-cli", "WAIT", "1", "5000")
                .getStdout().trim()).isEqualTo("1");

        master.stop();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            repository.writeConfig(
                    "sentinel-demo", "test", "default", "switch", "after", 2L
            );
            assertThat(repository.readConfigValue(
                    "sentinel-demo", "test", "default", "switch"
            )).isEqualTo("after");
            assertThat(repository.readConfigVersion(
                    "sentinel-demo", "test", "default", "switch"
            )).isEqualTo(2L);
        });
    }

    private GenericContainer<?> redis(String alias) {
        return new GenericContainer<>(REDIS_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(alias)
                .withExposedPorts(6379)
                .withCommand(
                        "redis-server",
                        "--appendonly", "no",
                        "--protected-mode", "no"
                )
                .waitingFor(Wait.forListeningPort());
    }

    private GenericContainer<?> sentinel(int index) {
        String alias = "redis-sentinel-" + index;
        String configuration = String.join("\n",
                "port 26379",
                "sentinel resolve-hostnames yes",
                "sentinel announce-hostnames yes",
                "sentinel monitor " + MASTER_NAME + " " + MASTER_ALIAS + " 6379 2",
                "sentinel down-after-milliseconds " + MASTER_NAME + " 1000",
                "sentinel failover-timeout " + MASTER_NAME + " 10000",
                "sentinel parallel-syncs " + MASTER_NAME + " 1",
                "sentinel announce-ip " + alias,
                "sentinel announce-port 26379",
                ""
        );
        return new GenericContainer<>(REDIS_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(alias)
                .withExposedPorts(26379)
                .withCopyToContainer(
                        Transferable.of(configuration),
                        "/etc/redis/sentinel.conf"
                )
                .withCommand("redis-sentinel", "/etc/redis/sentinel.conf")
                .waitingFor(Wait.forListeningPort());
    }

    private String address(GenericContainer<?> container) {
        return "redis://" + container.getHost() + ":" + container.getMappedPort(26379);
    }

    private HostPortNatMapper natMapper() {
        Map<String, String> mappings = new LinkedHashMap<>();
        addMapping(mappings, master, MASTER_ALIAS, 6379);
        addMapping(mappings, replica, REPLICA_ALIAS, 6379);
        for (int index = 0; index < sentinels.size(); index++) {
            addMapping(
                    mappings,
                    sentinels.get(index),
                    "redis-sentinel-" + (index + 1),
                    26379
            );
        }
        HostPortNatMapper mapper = new HostPortNatMapper();
        mapper.setHostsPortMap(mappings);
        return mapper;
    }

    private void addMapping(Map<String, String> mappings,
                            GenericContainer<?> container,
                            String alias,
                            int port) {
        String mapped = container.getHost() + ":" + container.getMappedPort(port);
        mappings.put(alias + ":" + port, mapped);
        mappings.put(containerIp(container) + ":" + port, mapped);
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
}
