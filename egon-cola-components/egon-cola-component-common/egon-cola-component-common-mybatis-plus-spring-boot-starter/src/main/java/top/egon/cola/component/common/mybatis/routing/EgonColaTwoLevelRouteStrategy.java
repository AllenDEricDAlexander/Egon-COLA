package top.egon.cola.component.common.mybatis.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery.OperationEnum;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;

/**
 * Shared deterministic address strategy for SQL guards and ShardingSphere adapters.
 * This class has no routing cache, JDBC dependency or key-generation responsibility.
 */
@Slf4j
@RequiredArgsConstructor
public final class EgonColaTwoLevelRouteStrategy {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    public EgonColaRouteResult route(EgonColaRoutingProfileBO profile, EgonColaRouteQuery query) {
        validationUtils.validate(profile);
        validationUtils.validate(query);
        return validationUtils.validate(calculate(profile, query));
    }

    /** Pure SPI candidate calculation. It cannot authorize a Query or Command SQL operation. */
    public static EgonColaRouteResult routeCandidates(EgonColaRoutingProfileBO profile, EgonColaRouteQuery query) {
        if (profile == null || query == null || query.tenantId() == null || query.operation() != OperationEnum.ROUTE_CANDIDATES) {
            throw new IllegalArgumentException("ROUTE_CANDIDATES_ONLY");
        }
        return calculate(profile, query);
    }

    private static EgonColaRouteResult calculate(EgonColaRoutingProfileBO profile, EgonColaRouteQuery query) {
        if (!profile.logicalTable().equals(query.logicalTable())) {
            throw new IllegalArgumentException("UNKNOWN_LOGICAL_TABLE");
        }
        if (query.tenantId() <= 0) {
            throw new IllegalArgumentException("SHARDING_KEY: tenant must be a positive Long");
        }
        List<EgonColaPhysicalTargetBO> targets = new ArrayList<>();
        switch (profile.kind()) {
            case SINGLE -> targets.addAll(profile.actualNodes().get(new PartitionKeyBO(0, 0)));
            case BROADCAST_READ_ONLY -> {
                if (query.operation() == OperationEnum.COMMAND) {
                    throw new IllegalArgumentException("BROADCAST_READ_ONLY");
                }
                targets.addAll(profile.actualNodes().get(new PartitionKeyBO(0, 0)));
            }
            case TENANT_LEGACY -> throw new IllegalArgumentException("TENANT_LEGACY requires the existing ShardingNodeMap adapter");
            case TENANT_ID_TWO_LEVEL -> {
                if (query.operation() == OperationEnum.COMMAND
                        && (query.rangeRequested() || query.secondaryValues().isEmpty())) {
                    throw new IllegalArgumentException("SHARDING_KEY: command requires exact tenant and secondary keys");
                }
                int slot = (int) Long.remainderUnsigned(mix64(query.tenantId()), profile.tenantSlotCount());
                TreeSet<Integer> buckets = new TreeSet<>();
                if (query.secondaryValues().isEmpty()) {
                    for (int b = 0; b < profile.secondaryBucketCount(); b++) {
                        buckets.add(b);
                    }
                } else {
                    for (Long root : query.secondaryValues()) {
                        buckets.add((int) Long.remainderUnsigned(mix64(root ^ profile.secondarySeed()), profile.secondaryBucketCount()));
                    }
                }
                for (int bucket : buckets) {
                    targets.addAll(profile.actualNodes().get(new PartitionKeyBO(slot, bucket)));
                }
            }
        }
        if (query.operation() == OperationEnum.QUERY && profile.kind() == TableKindEnum.TENANT_ID_TWO_LEVEL
                && targets.size() > profile.maxReadFanoutTables()) {
            throw new IllegalArgumentException("READ_FANOUT_LIMIT_EXCEEDED");
        }
        return new EgonColaRouteResult(targets, fingerprint(profile));
    }

    static long mix64(long key) {
        long z = key;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static String fingerprint(EgonColaRoutingProfileBO profile) {
        // Identifier/version constraints exclude delimiters; immutable maps have canonical key order.
        StringBuilder canonical = new StringBuilder("egon-route-v1|");
        canonical.append(profile.logicalTable()).append('|').append(profile.kind()).append('|')
                .append(profile.algorithmVersion()).append('|').append(profile.tenantSlotCount()).append('|')
                .append(profile.secondaryBucketCount()).append('|').append(profile.secondaryColumn()).append('|')
                .append(profile.rootKeyName()).append('|').append(Long.toUnsignedString(profile.secondarySeed())).append('|')
                .append(profile.maxReadFanoutTables()).append('|').append(profile.bindingGroup() == null ? "~" : profile.bindingGroup()).append('\n');
        profile.tenantSlotMap().forEach((slot, group) -> canonical.append(slot).append('=').append(group).append('\n'));
        profile.actualNodes().forEach((partition, nodes) -> {
            for (EgonColaPhysicalTargetBO node : nodes) {
                canonical.append(partition.tenantSlot()).append(',').append(partition.secondaryBucket()).append('=')
                        .append(node.group()).append('.').append(node.schema()).append('.').append(node.table()).append('\n');
            }
        });
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK must provide SHA-256", exception);
        }
    }
}
