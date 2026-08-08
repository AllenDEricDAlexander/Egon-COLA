package top.egon.cola.component.ddc.config;

import org.redisson.config.Config;

import java.util.List;
import java.util.Locale;

/**
 * 根据 DDC Redis 属性创建并校验 Redisson 拓扑配置。 Creates and validates a Redisson topology configuration from DDC Redis properties.
 */
public final class DdcRedisTopology {

    /** 禁止实例化 Redis 拓扑工具类。 Prevents instantiation of the Redis topology utility. */
    private DdcRedisTopology() {
    }

    /**
     * 创建 SINGLE、SENTINEL 或 CLUSTER Redisson 配置。 Creates a SINGLE, SENTINEL, or CLUSTER Redisson configuration.
     * @param mode 拓扑模式。 topology mode
     * @param nodes 节点 URL 列表。 node URL list
     * @param masterName 哨兵主节点名称。 sentinel master name
     * @param host 单机回退主机。 standalone fallback host
     * @param port 单机回退端口。 standalone fallback port
     * @param password 可选密码。 optional password
     * @param database 单机或哨兵逻辑数据库。 standalone or sentinel logical database
     * @return 已配置的 Redisson 配置。 configured Redisson configuration
     * @throws IllegalArgumentException 模式或所需连接参数无效时抛出。 thrown when the mode or required connection parameters are invalid
     */
    public static Config create(
            String mode,
            List<String> nodes,
            String masterName,
            String host,
            int port,
            String password,
            int database) {
        String topology = normalizeMode(mode);
        List<String> addresses = nodes == null
                ? List.of()
                : nodes.stream()
                .map(DdcRedisTopology::requireRedisUrl)
                .toList();
        Config config = new Config();
        switch (topology) {
            case "SENTINEL" -> sentinel(
                    config,
                    addresses,
                    masterName,
                    password,
                    database
            );
            case "CLUSTER" -> cluster(config, addresses, password);
            case "SINGLE" -> single(
                    config,
                    addresses,
                    host,
                    port,
                    password,
                    database
            );
            default -> throw new IllegalArgumentException(
                    "DDC Redis mode must be SINGLE, SENTINEL, or CLUSTER"
            );
        }
        return config;
    }

    /**
     * 将配置设为单机模式。 Configures standalone mode.
     * @param config 待修改配置。 configuration to mutate
     * @param addresses 可选单个节点 URL。 optional single node URL
     * @param host 回退主机。 fallback host
     * @param port 回退端口。 fallback port
     * @param password 可选密码。 optional password
     * @param database 逻辑数据库索引。 logical database index
     */
    private static void single(
            Config config,
            List<String> addresses,
            String host,
            int port,
            String password,
            int database) {
        if (addresses.size() > 1) {
            throw new IllegalArgumentException(
                    "DDC Redis SINGLE mode accepts at most one node URL"
            );
        }
        String address = addresses.isEmpty()
                ? singleAddress(host, port)
                : addresses.getFirst();
        var single = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);
        if (hasText(password)) {
            single.setPassword(password);
        }
    }

    /**
     * 将配置设为哨兵模式。 Configures sentinel mode.
     * @param config 待修改配置。 configuration to mutate
     * @param addresses 哨兵节点 URL。 sentinel node URLs
     * @param masterName 主节点名称。 master name
     * @param password 可选密码。 optional password
     * @param database 逻辑数据库索引。 logical database index
     */
    private static void sentinel(
            Config config,
            List<String> addresses,
            String masterName,
            String password,
            int database) {
        if (!hasText(masterName)) {
            throw new IllegalArgumentException(
                    "DDC Redis Sentinel master name is required"
            );
        }
        if (addresses.isEmpty()) {
            throw new IllegalArgumentException(
                    "DDC Redis Sentinel node URLs are required"
            );
        }
        var sentinel = config.useSentinelServers()
                .setMasterName(masterName.trim())
                .setDatabase(database)
                .addSentinelAddress(addresses.toArray(String[]::new));
        if (hasText(password)) {
            sentinel.setPassword(password);
        }
    }

    /**
     * 将配置设为集群模式。 Configures cluster mode.
     * @param config 待修改配置。 configuration to mutate
     * @param addresses 集群节点 URL。 cluster node URLs
     * @param password 可选密码。 optional password
     */
    private static void cluster(
            Config config,
            List<String> addresses,
            String password) {
        if (addresses.isEmpty()) {
            throw new IllegalArgumentException(
                    "DDC Redis Cluster node URLs are required"
            );
        }
        var cluster = config.useClusterServers()
                .addNodeAddress(addresses.toArray(String[]::new));
        if (hasText(password)) {
            cluster.setPassword(password);
        }
    }

    /**
     * 从主机和端口构造单机 Redis URL。 Builds a standalone Redis URL from host and port.
     * @param host Redis 主机。 Redis host
     * @param port Redis 端口。 Redis port
     * @return redis:// URL。 redis:// URL
     * @throws IllegalArgumentException 主机为空或端口越界时抛出。 thrown when the host is blank or port is out of range
     */
    private static String singleAddress(String host, int port) {
        if (!hasText(host) || port <= 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "DDC Redis single server host and port are invalid"
            );
        }
        return "redis://" + host.trim() + ":" + port;
    }

    /**
     * 校验并规范化 Redis 节点 URL。 Validates and normalizes a Redis node URL.
     * @param value 节点 URL。 node URL
     * @return 去除首尾空白的 redis:// 或 rediss:// URL。 trimmed redis:// or rediss:// URL
     * @throws IllegalArgumentException URL 缺失或协议不受支持时抛出。 thrown when the URL is absent or uses an unsupported scheme
     */
    private static String requireRedisUrl(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(
                    "DDC Redis node URL is required"
            );
        }
        String address = value.trim();
        if (!address.startsWith("redis://")
                && !address.startsWith("rediss://")) {
            throw new IllegalArgumentException(
                    "DDC Redis nodes must use redis:// or rediss:// URLs"
            );
        }
        return address;
    }

    /**
     * 将拓扑模式规范化为大写，空值回退为 SINGLE。 Normalizes topology mode to uppercase, defaulting blank input to SINGLE.
     * @param mode 原始模式。 raw mode
     * @return 规范化模式。 normalized mode
     */
    private static String normalizeMode(String mode) {
        return hasText(mode)
                ? mode.trim().toUpperCase(Locale.ROOT)
                : "SINGLE";
    }

    /**
     * 判断文本是否非空白。 Determines whether text is non-blank.
     * @param value 待判断文本。 text to inspect
     * @return 非空白时为 {@code true}。 {@code true} when non-blank
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
