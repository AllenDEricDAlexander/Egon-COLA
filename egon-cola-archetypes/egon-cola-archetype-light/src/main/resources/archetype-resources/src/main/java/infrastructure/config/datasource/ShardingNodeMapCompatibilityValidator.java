package ${package}.infrastructure.config.datasource;

/**
 * Guards the append-only mapping contract required by a 2N expansion.
 */
public final class ShardingNodeMapCompatibilityValidator {

    public void assertCanExpandTo(ShardingNodeMap current, ShardingNodeMap candidate) {
        if (current == null || candidate == null) {
            throw new IllegalArgumentException("current and candidate node maps are required");
        }
        if (candidate.mappingVersion() <= current.mappingVersion()) {
            throw new IllegalArgumentException("mapping version must increase");
        }
        if (current.nodeCount() > Integer.MAX_VALUE / 2
                || candidate.nodeCount() != current.nodeCount() * 2) {
            throw new IllegalArgumentException("node count must expand from N to 2N");
        }
        current.nodes().forEach((slot, node) -> {
            if (!node.equals(candidate.nodes().get(slot))) {
                throw new IllegalArgumentException(
                        "existing slot mapping must not change: " + slot);
            }
        });
    }
}
