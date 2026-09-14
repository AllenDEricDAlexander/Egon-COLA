package top.egon.cola.component.common.mybatis.sharding.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Tenant-first then business-root two-level sharding using mix64-v1.
 */
@Slf4j
@Component("COMPLEX_TENANT_THEN_BUSINESS")
@RequiredArgsConstructor
public class EgonColaComplexTenantThenBusinessShardingStrategy implements EgonColaShardingStrategy {

    static final String TYPE = "COMPLEX_TENANT_THEN_BUSINESS";
    private static final String TABLE_ALGORITHM =
            "top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaTenantBusinessComplexShardingAlgorithm";
    private static final String DATABASE_ALGORITHM =
            "top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaLongTenantShardingAlgorithm";

    @Qualifier("egonColaTwoLevelRouteStrategy")
    private final EgonColaTwoLevelRouteStrategy routeStrategy;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void validateTable(TableProperties table) {
        EgonColaSingleTableShardingStrategy.requireType(table, TYPE);
        if (table.rootKeyName() == null || table.rootKeyName().isBlank()) {
            throw new EgonColaMybatisPlusConfigurationException("SHARDING_KEY");
        }
        List<String> columns = table.tableColumns() == null ? List.of() : table.tableColumns();
        if (!columns.contains("tenant_id") || !columns.contains(table.rootKeyName())) {
            throw new EgonColaMybatisPlusConfigurationException("SHARDING_KEY");
        }
        Objects.requireNonNull(routeStrategy, "egonColaTwoLevelRouteStrategy");
    }

    @Override
    public String toRulesFragment(String logicalTable, TableProperties table,
                                  List<PhysicalDataSourceProperties> dataSources) {
        EgonColaRoutingProfileBO profile = toRoutingProfile(logicalTable, table, dataSources);
        List<EgonColaPhysicalTargetBO> nodes = new ArrayList<>();
        profile.actualNodes().values().forEach(nodes::addAll);
        String actual = EgonColaShardingStrategyNodes.joinNodes(nodes);
        return "tables:\n  " + logicalTable + ":\n    actualDataNodes: " + actual
                + "\n    databaseStrategy:\n      standard:\n        shardingColumn: tenant_id\n"
                + "        shardingAlgorithmName: " + logicalTable + "_database\n"
                + "    tableStrategy:\n      complex:\n        shardingColumns: tenant_id,"
                + table.rootKeyName() + "\n        shardingAlgorithmName: " + logicalTable + "_table\n"
                + "shardingAlgorithms:\n  " + logicalTable + "_database:\n    type: CLASS_BASED\n"
                + "    props:\n      strategy: STANDARD\n      algorithmClassName: " + DATABASE_ALGORITHM + "\n"
                + "  " + logicalTable + "_table:\n    type: CLASS_BASED\n    props:\n      strategy: COMPLEX\n"
                + "      algorithmClassName: " + TABLE_ALGORITHM + "\n";
    }

    @Override
    public EgonColaRoutingProfileBO toRoutingProfile(String logicalTable, TableProperties table,
                                                     List<PhysicalDataSourceProperties> dataSources) {
        validateTable(table);
        List<String> groups = EgonColaShardingStrategyNodes.primaryGroups(dataSources);
        if (groups.isEmpty()) {
            throw new EgonColaMybatisPlusConfigurationException("BROADCAST_PRIMARY_REQUIRED");
        }
        int tenantSlotCount = nextPowerOfTwo(groups.size());
        int secondaryBucketCount = 2;
        Map<Integer, String> tenantSlotMap = new TreeMap<>();
        Map<PartitionKeyBO, List<EgonColaPhysicalTargetBO>> actualNodes = new LinkedHashMap<>();
        for (int slot = 0; slot < tenantSlotCount; slot++) {
            String group = groups.get(slot % groups.size());
            tenantSlotMap.put(slot, group);
            for (int bucket = 0; bucket < secondaryBucketCount; bucket++) {
                EgonColaPhysicalTargetBO node = new EgonColaPhysicalTargetBO(
                        group, "public", logicalTable + "_t" + slot + "_b" + bucket);
                actualNodes.put(new PartitionKeyBO(slot, bucket), List.of(node));
            }
        }
        return new EgonColaRoutingProfileBO(
                logicalTable,
                TableKindEnum.TENANT_ID_TWO_LEVEL,
                "mix64-v1",
                tenantSlotCount,
                secondaryBucketCount,
                tenantSlotMap,
                table.rootKeyName(),
                table.rootKeyName(),
                EgonColaShardingStrategyNodes.SEED,
                actualNodes,
                secondaryBucketCount,
                null);
    }

    private static int nextPowerOfTwo(int count) {
        int power = 1;
        while (power < count) {
            power <<= 1;
        }
        return power;
    }
}
