package top.egon.cola.component.common.mybatis.routing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Immutable table addresses. Changing any address requires a controlled data migration.
 */
public record EgonColaRoutingProfileBO(
        @NotBlank String logicalTable,
        @NotNull TableKindEnum kind,
        @NotBlank String algorithmVersion,
        @Min(1) @Max(1024) int tenantSlotCount,
        @Min(1) @Max(1024) int secondaryBucketCount,
        @NotNull Map<@NotNull Integer, @NotBlank String> tenantSlotMap,
        String secondaryColumn,
        String rootKeyName,
        long secondarySeed,
        @NotEmpty Map<@NotNull @Valid PartitionKeyBO, @NotEmpty List<@NotNull @Valid EgonColaPhysicalTargetBO>> actualNodes,
        @Min(1) @Max(1024) int maxReadFanoutTables,
        String bindingGroup) {

    static final long SECONDARY_SEED = 0x9e3779b97f4a7c15L;
    private static final Comparator<PartitionKeyBO> PARTITION_ORDER = Comparator
            .comparingInt(PartitionKeyBO::tenantSlot).thenComparingInt(PartitionKeyBO::secondaryBucket);

    public EgonColaRoutingProfileBO {
        EgonColaPhysicalTargetBO.requireIdentifier(logicalTable, "logical table");
        Objects.requireNonNull(kind, "kind");
        if (algorithmVersion == null || !algorithmVersion.matches("[a-z][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException("Invalid algorithm version");
        }
        requirePowerOfTwo(tenantSlotCount);
        requirePowerOfTwo(secondaryBucketCount);
        if ((long) tenantSlotCount * secondaryBucketCount > 4096 || maxReadFanoutTables < 1 || maxReadFanoutTables > 1024) {
            throw new IllegalArgumentException("Routing table or fanout limit exceeded");
        }
        if (secondarySeed != SECONDARY_SEED) {
            throw new IllegalArgumentException("Routing seed must remain fixed");
        }
        if (bindingGroup != null) {
            EgonColaPhysicalTargetBO.requireIdentifier(bindingGroup, "binding group");
        }
        tenantSlotMap = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(tenantSlotMap, "tenantSlotMap")));
        TreeMap<PartitionKeyBO, List<EgonColaPhysicalTargetBO>> copied = new TreeMap<>(PARTITION_ORDER);
        Objects.requireNonNull(actualNodes, "actualNodes").forEach((key, nodes) -> {
            Objects.requireNonNull(key, "partition key");
            if (nodes == null || nodes.isEmpty()) {
                throw new IllegalArgumentException("Partition must name actual nodes");
            }
            copied.put(key, List.copyOf(nodes).stream().sorted(EgonColaPhysicalTargetBO.ORDER).toList());
        });
        actualNodes = Collections.unmodifiableMap(copied);
        Set<EgonColaPhysicalTargetBO> uniqueNodes = new HashSet<>();
        for (List<EgonColaPhysicalTargetBO> nodes : actualNodes.values()) {
            for (EgonColaPhysicalTargetBO node : nodes) {
                if (!uniqueNodes.add(node)) {
                    throw new IllegalArgumentException("Duplicate physical node");
                }
            }
        }
        if (kind == TableKindEnum.TENANT_ID_TWO_LEVEL) {
            EgonColaPhysicalTargetBO.requireIdentifier(secondaryColumn, "secondary column");
            EgonColaPhysicalTargetBO.requireIdentifier(rootKeyName, "root key");
            if (!"mix64-v1".equals(algorithmVersion) || tenantSlotMap.size() != tenantSlotCount
                    || actualNodes.size() != tenantSlotCount * secondaryBucketCount) {
                throw new IllegalArgumentException("Incomplete two-level routing profile");
            }
            for (int t = 0; t < tenantSlotCount; t++) {
                String group = tenantSlotMap.get(t);
                if (group == null) {
                    throw new IllegalArgumentException("Missing tenant slot");
                }
                for (int b = 0; b < secondaryBucketCount; b++) {
                    List<EgonColaPhysicalTargetBO> nodes = actualNodes.get(new PartitionKeyBO(t, b));
                    if (nodes == null || nodes.size() != 1 || !group.equals(nodes.getFirst().group())) {
                        throw new IllegalArgumentException("Actual node does not match tenant slot");
                    }
                }
            }
        } else {
            if (tenantSlotCount != 1 || secondaryBucketCount != 1 || secondaryColumn != null || rootKeyName != null) {
                throw new IllegalArgumentException("Non-two-level profiles must use normalized dimensions and keys");
            }
            if (kind == TableKindEnum.SINGLE || kind == TableKindEnum.BROADCAST_READ_ONLY) {
                List<EgonColaPhysicalTargetBO> nodes = actualNodes.get(new PartitionKeyBO(0, 0));
                if (!tenantSlotMap.isEmpty() || actualNodes.size() != 1 || nodes == null
                        || (kind == TableKindEnum.SINGLE && nodes.size() != 1)) {
                    throw new IllegalArgumentException("Invalid single or broadcast topology");
                }
                Set<String> groups = new HashSet<>();
                for (EgonColaPhysicalTargetBO node : nodes) {
                    if (!groups.add(node.group()) || !logicalTable.equals(node.table())) {
                        throw new IllegalArgumentException("Broadcast copies must share the logical name in distinct groups");
                    }
                }
            }
        }
    }

    private static void requirePowerOfTwo(int count) {
        if (count < 1 || count > 1024 || (count & (count - 1)) != 0) {
            throw new IllegalArgumentException("Routing dimensions must be powers of two in 1..1024");
        }
    }

    public enum TableKindEnum {
        SINGLE, BROADCAST_READ_ONLY, TENANT_LEGACY, TENANT_ID_TWO_LEVEL
    }

    public record PartitionKeyBO(@Min(0) @Max(1023) int tenantSlot, @Min(0) @Max(1023) int secondaryBucket) {
        public PartitionKeyBO {
            if (tenantSlot < 0 || tenantSlot > 1023 || secondaryBucket < 0 || secondaryBucket > 1023) {
                throw new IllegalArgumentException("Partition index outside supported range");
            }
        }
    }
}
