package top.egon.cola.component.outbox.migration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;

import javax.sql.DataSource;
import java.util.Map;

/** Adds managed outbox DDL initialization before the Common logical datasource factory. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class OutboxLogicalDataSourceFactory implements LogicalDataSourceFactory {

    @Qualifier("outboxManagedDdlInitializer")
    private final OutboxManagedDdlInitializer ddlInitializer;

    @Override
    public DataSource create(@NotEmpty Map<@NotBlank String, @NotNull DataSource> physical,
                             @NotEmpty byte[] yaml) throws Exception {
        ddlInitializer.initialize(physical, yaml);
        DataSource logical = YamlShardingSphereDataSourceFactory.createDataSource(physical, yaml);
        if (logical == null) {
            throw new IllegalStateException("OUTBOX_LOGICAL_DATASOURCE_REQUIRED");
        }
        log.info("outbox logical datasource created after managed DDL initialization");
        return logical;
    }
}
