package top.egon.cola.component.common.mybatis.sharding.strategy;

import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;

import java.util.List;

/**
 * Table-kind strategy that emits ShardingSphere rule fragments and routing profiles.
 */
public interface EgonColaShardingStrategy {

    String type();

    void validateTable(EgonColaShardingProperties.TableProperties table);

    String toRulesFragment(String logicalTable, EgonColaShardingProperties.TableProperties table,
                           List<EgonColaShardingProperties.PhysicalDataSourceProperties> dataSources);

    EgonColaRoutingProfileBO toRoutingProfile(String logicalTable, EgonColaShardingProperties.TableProperties table,
                                              List<EgonColaShardingProperties.PhysicalDataSourceProperties> dataSources);
}
