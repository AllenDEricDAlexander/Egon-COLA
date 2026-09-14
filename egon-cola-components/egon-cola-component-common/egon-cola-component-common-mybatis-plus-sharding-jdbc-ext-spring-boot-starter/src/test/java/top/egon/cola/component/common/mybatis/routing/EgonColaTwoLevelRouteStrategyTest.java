package top.egon.cola.component.common.mybatis.routing;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery.OperationEnum;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EgonColaTwoLevelRouteStrategyTest {

    private static final long SEED = 0x9e3779b97f4a7c15L;
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final EgonColaTwoLevelRouteStrategy strategy = new EgonColaTwoLevelRouteStrategy(
            new ValidationUtils(VALIDATORS.getValidator()));

    @AfterAll
    static void closeValidators() {
        VALIDATORS.close();
    }

    @Test
    void routesGoldenVectorsToExactConfiguredNodes() {
        EgonColaRoutingProfileBO profile = profile("orders", "id", 16, 8, 8);
        long[] keys = {1, 2, 41, Long.MAX_VALUE};
        int[] slots = {5, 10, 5, 13};
        int[] buckets = {0, 6, 4, 0};
        for (int i = 0; i < keys.length; i++) {
            EgonColaRouteResult result = strategy.route(profile, query("orders", keys[i], List.of(keys[i]), OperationEnum.COMMAND, false));
            assertThat(result.targets()).containsExactly(target("orders", slots[i], buckets[i]));
            assertThat(result.fingerprint()).matches("[0-9a-f]{64}");
        }
    }

    @Test
    void distributesFixedTenantAndSnowflakeShapedSamplesWithinFivePercent() {
        int[] tenants = new int[16];
        int[] roots = new int[8];
        for (long i = 1; i <= 100_000; i++) {
            tenants[(int) Long.remainderUnsigned(EgonColaTwoLevelRouteStrategy.mix64(i), 16)]++;
            long snowflakeId = (i << 22) | (7L << 12) | (i % 4096);
            roots[(int) Long.remainderUnsigned(EgonColaTwoLevelRouteStrategy.mix64(snowflakeId ^ SEED), 8)]++;
        }
        for (int count : tenants) {
            assertThat(Math.abs(count - 6250.0) / 6250.0).isLessThanOrEqualTo(0.05);
        }
        for (int count : roots) {
            assertThat(Math.abs(count - 12500.0) / 12500.0).isLessThanOrEqualTo(0.05);
        }
    }

    @Test
    void tenantOnlyAndRangeReadsStayWithinOneTenantSlotAndDatabase() {
        EgonColaRoutingProfileBO profile = profile("orders", "id", 16, 8, 8);
        EgonColaRouteResult missing = strategy.route(profile, query("orders", 41L, List.of(), OperationEnum.QUERY, false));
        EgonColaRouteResult range = strategy.route(profile, query("orders", 41L, List.of(1L), OperationEnum.QUERY, true));
        assertThat(missing.targets()).hasSize(8).allSatisfy(node -> {
            assertThat(node.group()).isEqualTo("group1");
            assertThat(node.table()).startsWith("orders_t5_b");
        });
        assertThat(range).isEqualTo(missing);
    }

    @Test
    void boundsReadsAndDoesNotTreatCandidateCalculationAsCommandPermission() {
        EgonColaRoutingProfileBO profile = profile("orders", "id", 4, 2, 1);
        EgonColaRouteQuery candidates = query("orders", 41L, List.of(), OperationEnum.ROUTE_CANDIDATES, false);
        assertThat(strategy.route(profile, candidates).targets()).hasSize(2);
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.route(profile,
                query("orders", 41L, List.of(), OperationEnum.QUERY, false))).withMessageContaining("FANOUT");
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.route(profile,
                query("orders", 41L, List.of(), OperationEnum.COMMAND, false))).withMessageContaining("SHARDING_KEY");
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.route(profile,
                query("orders", 41L, List.of(1L), OperationEnum.COMMAND, true))).withMessageContaining("SHARDING_KEY");
    }

    @Test
    void deduplicatesInKeysAndTargetsAndUsesTheRootKeyForBoundTables() {
        EgonColaRoutingProfileBO orders = profile("orders", "id", 16, 8, 8);
        EgonColaRoutingProfileBO items = profile("order_items", "order_id", 16, 8, 8);
        EgonColaRouteQuery query = query("orders", 41L, List.of(2L, 1L, 2L, 1L), OperationEnum.COMMAND, false);
        assertThat(query.secondaryValues()).containsExactly(1L, 2L);
        List<EgonColaPhysicalTargetBO> parent = strategy.route(orders, query).targets();
        List<EgonColaPhysicalTargetBO> child = strategy.route(items,
                query("order_items", 41L, List.of(1L, 2L), OperationEnum.COMMAND, false)).targets();
        assertThat(parent).hasSize(2);
        for (int i = 0; i < parent.size(); i++) {
            assertThat(child.get(i).group()).isEqualTo(parent.get(i).group());
            assertThat(child.get(i).table()).isEqualTo(parent.get(i).table().replace("orders", "order_items"));
        }
    }

    @Test
    void rejectsInvalidAndUnregisteredKeys() {
        EgonColaRoutingProfileBO profile = profile("orders", "id", 4, 2, 2);
        for (Long tenant : new Long[]{null, 0L, -1L}) {
            assertThatThrownBy(() -> strategy.route(profile, query("orders", tenant, List.of(1L), OperationEnum.QUERY, false)))
                    .isInstanceOfAny(IllegalArgumentException.class, jakarta.validation.ConstraintViolationException.class);
        }
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.route(profile,
                query("unknown", 1L, List.of(1L), OperationEnum.QUERY, false)));
        for (long root : new long[]{0, -1}) {
            assertThatThrownBy(() -> strategy.route(profile, query("orders", 1L, List.of(root), OperationEnum.COMMAND, false)))
                    .isInstanceOfAny(IllegalArgumentException.class, jakarta.validation.ConstraintViolationException.class);
        }
        assertThatIllegalArgumentException().isThrownBy(() -> query("orders", 1L,
                Collections.nCopies(10_001, 1L), OperationEnum.QUERY, false));
        assertThatIllegalArgumentException().isThrownBy(() -> new EgonColaPhysicalTargetBO("group0", "public", "orders;drop table users"));
    }

    @Test
    void rejectsBrokenTopologyInsteadOfGuessingTargets() {
        EgonColaRoutingProfileBO valid = profile("orders", "id", 4, 2, 2);
        Map<Integer, String> missingSlot = new LinkedHashMap<>(valid.tenantSlotMap());
        missingSlot.remove(3);
        assertThatIllegalArgumentException().isThrownBy(() -> copy(valid, missingSlot, valid.actualNodes(), SEED, "mix64-v1"));
        Map<PartitionKeyBO, List<EgonColaPhysicalTargetBO>> nodes = new LinkedHashMap<>(valid.actualNodes());
        nodes.remove(new PartitionKeyBO(3, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> copy(valid, valid.tenantSlotMap(), nodes, SEED, "mix64-v1"));
        nodes.put(new PartitionKeyBO(3, 1), valid.actualNodes().get(new PartitionKeyBO(3, 0)));
        assertThatIllegalArgumentException().isThrownBy(() -> copy(valid, valid.tenantSlotMap(), nodes, SEED, "mix64-v1"));
        nodes.put(new PartitionKeyBO(3, 1), List.of(new EgonColaPhysicalTargetBO("group0", "public", "orders_t3_b1")));
        assertThatIllegalArgumentException().isThrownBy(() -> copy(valid, valid.tenantSlotMap(), nodes, SEED, "mix64-v1"));
        assertThatIllegalArgumentException().isThrownBy(() -> copy(valid, valid.tenantSlotMap(), valid.actualNodes(), 0, "mix64-v1"));
        assertThatIllegalArgumentException().isThrownBy(() -> copy(valid, valid.tenantSlotMap(), valid.actualNodes(), SEED, "random-v2"));
        assertThatIllegalArgumentException().isThrownBy(() -> profile("orders", "id", 3, 2, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> profile("orders", "id", 128, 64, 64));
    }

    @Test
    void fingerprintIsIndependentOfCollectionOrderButDetectsAddressChanges() {
        EgonColaRoutingProfileBO valid = profile("orders", "id", 4, 2, 2);
        List<Integer> slots = new ArrayList<>(valid.tenantSlotMap().keySet());
        Collections.reverse(slots);
        Map<Integer, String> reversedSlots = new LinkedHashMap<>();
        slots.forEach(slot -> reversedSlots.put(slot, valid.tenantSlotMap().get(slot)));
        List<PartitionKeyBO> keys = new ArrayList<>(valid.actualNodes().keySet());
        Collections.reverse(keys);
        Map<PartitionKeyBO, List<EgonColaPhysicalTargetBO>> reversedNodes = new LinkedHashMap<>();
        keys.forEach(key -> reversedNodes.put(key, new ArrayList<>(valid.actualNodes().get(key))));
        EgonColaRoutingProfileBO reordered = copy(valid, reversedSlots, reversedNodes, SEED, "mix64-v1");
        EgonColaRouteQuery query = query("orders", 1L, List.of(1L), OperationEnum.QUERY, false);
        String fingerprint = strategy.route(valid, query).fingerprint();
        assertThat(strategy.route(reordered, query).fingerprint()).isEqualTo(fingerprint);
        reversedSlots.clear();
        reversedNodes.clear();
        assertThat(strategy.route(reordered, query).fingerprint()).isEqualTo(fingerprint);
        Map<PartitionKeyBO, List<EgonColaPhysicalTargetBO>> renamed = new LinkedHashMap<>(valid.actualNodes());
        renamed.put(new PartitionKeyBO(0, 0), List.of(new EgonColaPhysicalTargetBO("group0", "public", "replacement")));
        assertThat(strategy.route(copy(valid, valid.tenantSlotMap(), renamed, SEED, "mix64-v1"), query).fingerprint()).isNotEqualTo(fingerprint);
        assertThat(strategy.route(profile("orders", "order_id", 4, 2, 2), query).fingerprint()).isNotEqualTo(fingerprint);
        assertThatThrownBy(() -> reordered.actualNodes().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> reordered.actualNodes().get(new PartitionKeyBO(0, 0)).clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> strategy.route(valid, query).targets().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void singleAndBroadcastRemainTenantScopedAndBroadcastRejectsWrites() {
        EgonColaPhysicalTargetBO first = new EgonColaPhysicalTargetBO("group0", "public", "metadata");
        EgonColaRoutingProfileBO single = simpleProfile(TableKindEnum.SINGLE, List.of(first));
        assertThat(strategy.route(single, query("metadata", 1L, List.of(), OperationEnum.COMMAND, false)).targets()).containsExactly(first);
        EgonColaRoutingProfileBO broadcast = simpleProfile(TableKindEnum.BROADCAST_READ_ONLY,
                List.of(first, new EgonColaPhysicalTargetBO("group1", "public", "metadata")));
        assertThat(strategy.route(broadcast, query("metadata", 1L, List.of(), OperationEnum.QUERY, false)).targets()).hasSize(2);
        assertThatIllegalArgumentException().isThrownBy(() -> strategy.route(broadcast,
                query("metadata", 1L, List.of(), OperationEnum.COMMAND, false))).withMessageContaining("BROADCAST_READ_ONLY");
    }

    @Test
    void distinguishesAbsentBindingGroupFromLiteralNullNameInFingerprint() {
        EgonColaRoutingProfileBO original = simpleProfile(TableKindEnum.SINGLE,
                List.of(new EgonColaPhysicalTargetBO("group0", "public", "metadata")));
        EgonColaRoutingProfileBO named = new EgonColaRoutingProfileBO(original.logicalTable(), original.kind(),
                original.algorithmVersion(), 1, 1, Map.of(), null, null, SEED, original.actualNodes(), 8, "null");
        EgonColaRouteQuery query = query("metadata", 1L, List.of(), OperationEnum.QUERY, false);
        assertThat(strategy.route(original, query).fingerprint()).isNotEqualTo(strategy.route(named, query).fingerprint());
    }

    @Test
    void aHotTenantStillOccupiesOneDatabaseEvenWhenItsRootsSpreadAcrossBuckets() {
        EgonColaRoutingProfileBO profile = profile("orders", "id", 16, 8, 8);
        List<Long> hotRoots = java.util.stream.LongStream.rangeClosed(1, 900).boxed().toList();
        List<EgonColaPhysicalTargetBO> hot = strategy.route(profile,
                query("orders", 41L, hotRoots, OperationEnum.COMMAND, false)).targets();
        assertThat(hot).hasSize(8).allSatisfy(node -> assertThat(node.group()).isEqualTo("group1"));
        // 90% of a 1000-row workload is still pinned to one database; hashing cannot remove tenant skew.
        assertThat(hotRoots).hasSize(900);
    }

    @Test
    void unmanagedSpiCandidateCalculationCannotImpersonateACommand() {
        var profile = profile("orders", "id", 4, 2, 2);
        var candidates = query("orders", 41L, List.of(1L), OperationEnum.ROUTE_CANDIDATES, false);
        assertThat(EgonColaTwoLevelRouteStrategy.routeCandidates(profile, candidates)).isEqualTo(strategy.route(profile, candidates));
        assertThatIllegalArgumentException().isThrownBy(() -> EgonColaTwoLevelRouteStrategy.routeCandidates(profile,
                query("orders", 41L, List.of(1L), OperationEnum.COMMAND, false))).withMessage("ROUTE_CANDIDATES_ONLY");
    }

    private static EgonColaRoutingProfileBO simpleProfile(TableKindEnum kind, List<EgonColaPhysicalTargetBO> nodes) {
        return new EgonColaRoutingProfileBO("metadata", kind, "static-v1", 1, 1, Map.of(), null, null,
                SEED, Map.of(new PartitionKeyBO(0, 0), nodes), 8, null);
    }

    private static EgonColaRoutingProfileBO profile(String table, String column, int slots, int buckets, int fanout) {
        Map<Integer, String> map = new LinkedHashMap<>();
        Map<PartitionKeyBO, List<EgonColaPhysicalTargetBO>> nodes = new LinkedHashMap<>();
        for (int t = 0; t < slots; t++) {
            map.put(t, "group" + (t % 2));
            for (int b = 0; b < buckets; b++) {
                nodes.put(new PartitionKeyBO(t, b), List.of(target(table, t, b)));
            }
        }
        return new EgonColaRoutingProfileBO(table, TableKindEnum.TENANT_ID_TWO_LEVEL, "mix64-v1", slots, buckets,
                map, column, "order", SEED, nodes, fanout, "orders_items");
    }

    private static EgonColaRoutingProfileBO copy(EgonColaRoutingProfileBO profile, Map<Integer, String> map,
            Map<PartitionKeyBO, List<EgonColaPhysicalTargetBO>> nodes, long seed, String version) {
        return new EgonColaRoutingProfileBO(profile.logicalTable(), profile.kind(), version, profile.tenantSlotCount(),
                profile.secondaryBucketCount(), map, profile.secondaryColumn(), profile.rootKeyName(), seed, nodes,
                profile.maxReadFanoutTables(), profile.bindingGroup());
    }

    private static EgonColaPhysicalTargetBO target(String table, int slot, int bucket) {
        return new EgonColaPhysicalTargetBO("group" + (slot % 2), "public", table + "_t" + slot + "_b" + bucket);
    }

    private static EgonColaRouteQuery query(String table, Long tenant, List<Long> roots, OperationEnum operation, boolean range) {
        return new EgonColaRouteQuery(table, tenant, roots, operation, range);
    }
}
