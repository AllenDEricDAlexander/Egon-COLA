package top.egon.cola.component.ddc.config;

import org.redisson.config.Config;

import java.util.List;
import java.util.Locale;

public final class DdcRedisTopology {

    private DdcRedisTopology() {
    }

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

    private static String singleAddress(String host, int port) {
        if (!hasText(host) || port <= 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "DDC Redis single server host and port are invalid"
            );
        }
        return "redis://" + host.trim() + ":" + port;
    }

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

    private static String normalizeMode(String mode) {
        return hasText(mode)
                ? mode.trim().toUpperCase(Locale.ROOT)
                : "SINGLE";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
