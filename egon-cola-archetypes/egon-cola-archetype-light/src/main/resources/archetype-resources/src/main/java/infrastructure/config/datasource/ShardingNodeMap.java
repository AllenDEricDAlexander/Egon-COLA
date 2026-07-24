package ${package}.infrastructure.config.datasource;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Immutable stable-slot mapping used by UUIDv7 sharding.
 */
public record ShardingNodeMap(
        int mappingVersion,
        int nodeCount,
        Map<Integer, PhysicalNode> nodes) {

    public ShardingNodeMap {
        if (mappingVersion < 1) {
            throw new IllegalArgumentException("mapping version must be positive");
        }
        if (nodeCount < 2 || !isPowerOfTwo(nodeCount)) {
            throw new IllegalArgumentException("node count must be a power of two and at least 2");
        }
        if (nodes == null || nodes.size() != nodeCount) {
            throw new IllegalArgumentException("node map size must match node count");
        }

        Map<Integer, PhysicalNode> sortedNodes = new TreeMap<>(nodes);
        for (int slot = 0; slot < nodeCount; slot++) {
            if (!sortedNodes.containsKey(slot)) {
                throw new IllegalArgumentException("node map slots must be continuous from zero");
            }
        }
        if (new HashSet<>(sortedNodes.values()).size() != nodeCount) {
            throw new IllegalArgumentException("physical nodes must be unique");
        }
        validateTopology(sortedNodes);
        nodes = Collections.unmodifiableMap(new LinkedHashMap<>(sortedNodes));
    }

    public static ShardingNodeMap parse(Properties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("sharding properties must not be null");
        }
        return parse(
                properties.getProperty("mapping-version"),
                properties.getProperty("node-count"),
                properties.getProperty("node-map"));
    }

    static ShardingNodeMap parse(String mappingVersion, String nodeCount, String nodeMap) {
        int parsedMappingVersion = parseInteger(mappingVersion, "mapping version");
        int parsedNodeCount = parseInteger(nodeCount, "node count");
        if (nodeMap == null || nodeMap.isBlank()) {
            throw new IllegalArgumentException("node map must not be blank");
        }

        Map<Integer, PhysicalNode> nodes = new LinkedHashMap<>();
        for (String mapping : nodeMap.split(",")) {
            String[] slotAndNode = mapping.trim().split("=", -1);
            if (slotAndNode.length != 2) {
                throw new IllegalArgumentException("node map entry must use slot=database:suffix");
            }
            int slot = parseInteger(slotAndNode[0].trim(), "slot");
            String physicalNode = slotAndNode[1].trim();
            int separator = physicalNode.lastIndexOf(':');
            if (separator < 1 || separator == physicalNode.length() - 1) {
                throw new IllegalArgumentException("physical node must use database:suffix");
            }
            PhysicalNode previous = nodes.put(
                    slot,
                    new PhysicalNode(
                            physicalNode.substring(0, separator).trim(),
                            parseInteger(physicalNode.substring(separator + 1).trim(), "table suffix")));
            if (previous != null) {
                throw new IllegalArgumentException("node map slots must be unique");
            }
        }
        return new ShardingNodeMap(parsedMappingVersion, parsedNodeCount, nodes);
    }

    public PhysicalNode route(String shardingKey) {
        return nodes.get(routeSlot(shardingKey));
    }

    public int routeSlot(String shardingKey) {
        UUID uuid = parseUuidV7(shardingKey);
        String canonicalKey = uuid.toString();
        int hash = canonicalKey.hashCode();
        int spreadHash = hash ^ (hash >>> 16);
        return spreadHash & (nodeCount - 1);
    }

    private static UUID parseUuidV7(String shardingKey) {
        if (shardingKey == null || shardingKey.isBlank()) {
            throw new IllegalArgumentException("sharding key must be UUIDv7");
        }
        try {
            UUID uuid = UUID.fromString(shardingKey);
            if (uuid.version() != 7 || !uuid.toString().equals(shardingKey)) {
                throw new IllegalArgumentException("sharding key must be UUIDv7");
            }
            return uuid;
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("sharding key must be UUIDv7");
        }
    }

    private static void validateTopology(Map<Integer, PhysicalNode> nodes) {
        Map<String, Set<Integer>> suffixesByDatabase = new TreeMap<>();
        nodes.values().forEach(node -> suffixesByDatabase
                .computeIfAbsent(node.database(), ignored -> new TreeSet<>())
                .add(node.tableSuffix()));
        if (!isPowerOfTwo(suffixesByDatabase.size())) {
            throw new IllegalArgumentException("database count must be a power of two");
        }

        Integer tablesPerDatabase = null;
        for (Set<Integer> suffixes : suffixesByDatabase.values()) {
            if (tablesPerDatabase == null) {
                tablesPerDatabase = suffixes.size();
            }
            if (suffixes.size() != tablesPerDatabase || !isPowerOfTwo(suffixes.size())) {
                throw new IllegalArgumentException(
                        "table count per database must be equal and a power of two");
            }
            for (int suffix = 0; suffix < suffixes.size(); suffix++) {
                if (!suffixes.contains(suffix)) {
                    throw new IllegalArgumentException(
                            "table suffixes must be continuous from zero");
                }
            }
        }
    }

    private static int parseInteger(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(propertyName + " must not be blank");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(propertyName + " must be an integer");
        }
    }

    private static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    public record PhysicalNode(String database, int tableSuffix) {

        public PhysicalNode {
            if (database == null || !database.matches("shard_\\d+")) {
                throw new IllegalArgumentException("database must use shard_N naming");
            }
            if (tableSuffix < 0) {
                throw new IllegalArgumentException("table suffix must not be negative");
            }
        }
    }
}
