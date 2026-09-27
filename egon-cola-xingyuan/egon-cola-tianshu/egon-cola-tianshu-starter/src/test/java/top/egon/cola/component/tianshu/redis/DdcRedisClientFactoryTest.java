package top.egon.cola.component.tianshu.redis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcRedisClientFactoryTest {

    @Test
    void createsSentinelAndClusterTopologiesFromRedisUrls()
            throws Exception {
        var sentinel = DdcRedisClientFactory.configuration(
                "SENTINEL",
                List.of(
                        "redis://redis-sentinel-1:26379",
                        "redis://redis-sentinel-2:26379"
                ),
                "tianshu-master",
                "ignored",
                6379,
                "secret",
                2
        );
        assertThat(sentinel.isSentinelConfig()).isTrue();
        assertThat(sentinel.toYAML())
                .contains("masterName: \"tianshu-master\"")
                .contains("redis://redis-sentinel-1:26379")
                .contains("redis://redis-sentinel-2:26379")
                .contains("database: 2");

        var cluster = DdcRedisClientFactory.configuration(
                "CLUSTER",
                List.of(
                        "rediss://redis-cluster-1:6379",
                        "rediss://redis-cluster-2:6379"
                ),
                "",
                "ignored",
                6379,
                null,
                0
        );
        assertThat(cluster.isClusterConfig()).isTrue();
        assertThat(cluster.toYAML())
                .contains("rediss://redis-cluster-1:6379")
                .contains("rediss://redis-cluster-2:6379");
    }

    @Test
    void appliesTenantAclUsernameToEveryTopology() throws Exception {
        var single = DdcRedisClientFactory.configuration(
                "SINGLE", List.of(), "", "redis-single", 6379,
                "password", "tenant-user", 0);
        var sentinel = DdcRedisClientFactory.configuration(
                "SENTINEL", List.of("redis://sentinel:26379"), "master",
                "ignored", 6379, "password", "tenant-user", 0);
        var cluster = DdcRedisClientFactory.configuration(
                "CLUSTER", List.of("redis://cluster:6379"), "", "ignored",
                6379, "password", "tenant-user", 0);

        assertThat(single.toYAML()).contains("username: \"tenant-user\"");
        assertThat(sentinel.toYAML()).contains("username: \"tenant-user\"");
        assertThat(cluster.toYAML()).contains("username: \"tenant-user\"");
    }

    @Test
    void rejectsSentinelWithoutMasterAndNonUrlClusterNodes() {
        assertThatThrownBy(() -> DdcRedisClientFactory.configuration(
                "SENTINEL",
                List.of("redis://redis-sentinel:26379"),
                "",
                "ignored",
                6379,
                null,
                0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("master");

        assertThatThrownBy(() -> DdcRedisClientFactory.configuration(
                "CLUSTER",
                List.of("redis-cluster:6379"),
                "",
                "ignored",
                6379,
                null,
                0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("redis://");
    }
}
