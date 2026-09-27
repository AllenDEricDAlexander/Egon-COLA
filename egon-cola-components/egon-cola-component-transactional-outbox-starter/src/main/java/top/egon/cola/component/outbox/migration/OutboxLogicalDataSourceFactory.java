package top.egon.cola.component.outbox.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;

import javax.sql.DataSource;
import java.util.Map;

/** Adds managed outbox DDL initialization before the Common logical datasource factory. */
@Slf4j
@RequiredArgsConstructor
public class OutboxLogicalDataSourceFactory implements LogicalDataSourceFactory {

    @Qualifier("outboxManagedDdlInitializer")
    private final OutboxManagedDdlInitializer ddlInitializer;

    @Override
    public DataSource create(Map<String, DataSource> physical, byte[] yaml) throws Exception {
        ddlInitializer.initialize(physical, yaml);
        DataSource logical = YamlShardingSphereDataSourceFactory.createDataSource(physical, yaml);
        if (logical == null) {
            throw new IllegalStateException("OUTBOX_LOGICAL_DATASOURCE_REQUIRED");
        }
        log.info("outbox logical datasource created after managed DDL initialization");
        return logical;
    }
}
