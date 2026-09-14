package top.egon.cola.component.common.mybatis.sharding.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;

import java.util.List;

/**
 * Adapter for a native ShardingSphere rules YAML. Profiles are produced when the resource is loaded.
 */
@Slf4j
@Component("NATIVE")
@RequiredArgsConstructor
public class EgonColaNativeYamlShardingStrategy implements EgonColaShardingStrategy {

    static final String TYPE = "NATIVE";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void validateTable(TableProperties table) {
        EgonColaSingleTableShardingStrategy.requireType(table, TYPE);
    }

    @Override
    public String toRulesFragment(String logicalTable, TableProperties table,
                                  List<PhysicalDataSourceProperties> dataSources) {
        validateTable(table);
        return "";
    }

    @Override
    public EgonColaRoutingProfileBO toRoutingProfile(String logicalTable, TableProperties table,
                                                     List<PhysicalDataSourceProperties> dataSources) {
        validateTable(table);
        throw new EgonColaMybatisPlusConfigurationException("NATIVE_YAML_REQUIRED");
    }
}
