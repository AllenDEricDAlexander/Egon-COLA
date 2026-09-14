package top.egon.cola.component.common.mybatis.sharding.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One-level tenant_id standard sharding, mapped to the existing TENANT_LEGACY profile kind.
 */
@Slf4j
@Component("STANDARD_TENANT_ID")
@RequiredArgsConstructor
public class EgonColaStandardTenantIdShardingStrategy implements EgonColaShardingStrategy {

    static final String TYPE = "STANDARD_TENANT_ID";
    private static final String ALGORITHM =
            "top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaLongTenantShardingAlgorithm";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void validateTable(TableProperties table) {
        EgonColaSingleTableShardingStrategy.requireType(table, TYPE);
        String column = table.shardingColumn() == null || table.shardingColumn().isBlank()
                ? "tenant_id" : table.shardingColumn();
        if (!"tenant_id".equals(column)) {
            throw new EgonColaMybatisPlusConfigurationException("SHARDING_KEY");
        }
    }

    @Override
    public String toRulesFragment(String logicalTable, TableProperties table,
                                  List<PhysicalDataSourceProperties> dataSources) {
        EgonColaRoutingProfileBO profile = toRoutingProfile(logicalTable, table, dataSources);
        List<EgonColaPhysicalTargetBO> nodes = profile.actualNodes().get(new PartitionKeyBO(0, 0));
        String actual = EgonColaShardingStrategyNodes.joinNodes(nodes);
        return "tables:\n  " + logicalTable + ":\n    actualDataNodes: " + actual
                + "\n    databaseStrategy:\n      standard:\n        shardingColumn: tenant_id\n"
                + "        shardingAlgorithmName: " + logicalTable + "_database\n"
                + "    tableStrategy:\n      standard:\n        shardingColumn: tenant_id\n"
                + "        shardingAlgorithmName: " + logicalTable + "_table\n"
                + "shardingAlgorithms:\n  " + logicalTable + "_database:\n    type: CLASS_BASED\n"
                + "    props:\n      strategy: STANDARD\n      algorithmClassName: " + ALGORITHM + "\n";
    }

    @Override
    public EgonColaRoutingProfileBO toRoutingProfile(String logicalTable, TableProperties table,
                                                     List<PhysicalDataSourceProperties> dataSources) {
        validateTable(table);
        List<String> groups = EgonColaShardingStrategyNodes.primaryGroups(dataSources);
        if (groups.isEmpty()) {
            throw new EgonColaMybatisPlusConfigurationException("BROADCAST_PRIMARY_REQUIRED");
        }
        List<EgonColaPhysicalTargetBO> nodes = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            nodes.add(new EgonColaPhysicalTargetBO(groups.get(index), "public", logicalTable + "_t" + index));
        }
        return new EgonColaRoutingProfileBO(
                logicalTable,
                TableKindEnum.TENANT_LEGACY,
                "legacy-v1",
                1,
                1,
                Map.of(),
                null,
                null,
                EgonColaShardingStrategyNodes.SEED,
                Map.of(new PartitionKeyBO(0, 0), nodes),
                1,
                null);
    }
}
