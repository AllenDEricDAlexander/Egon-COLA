package top.egon.cola.component.common.mybatis.sharding.bootstrap;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Named physical Hikari pools owned by the logical ShardingSphere DataSource.
 */
@Slf4j
@Component("egonColaPhysicalDataSourceFactory")
public class EgonColaPhysicalDataSourceFactory {

    private final PhysicalDataSourceCreator creator;

    public EgonColaPhysicalDataSourceFactory() {
        this(EgonColaPhysicalDataSourceFactory::createHikariDataSource);
    }

    EgonColaPhysicalDataSourceFactory(PhysicalDataSourceCreator creator) {
        this.creator = creator;
    }

    public Map<String, DataSource> create(EgonColaShardingProperties properties) {
        if (properties == null || properties.getDataSources() == null || properties.getDataSources().isEmpty()) {
            throw new EgonColaMybatisPlusConfigurationException("PHYSICAL_DATASOURCES_REQUIRED");
        }
        Map<String, DataSource> result = new LinkedHashMap<>();
        try {
            for (PhysicalDataSourceProperties source : properties.getDataSources()) {
                if (result.containsKey(source.name())) {
                    throw new EgonColaMybatisPlusConfigurationException("DUPLICATE_PHYSICAL_DATASOURCE");
                }
                result.put(source.name(), creator.create(source));
            }
            return Collections.unmodifiableMap(result);
        } catch (RuntimeException failure) {
            close(result.values());
            throw failure;
        }
    }

    public void close(Collection<? extends DataSource> dataSources) {
        if (dataSources == null) {
            return;
        }
        for (DataSource dataSource : dataSources) {
            if (dataSource instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                    log.warn("failed to close physical data source during startup cleanup");
                }
            }
        }
    }

    private static HikariDataSource createHikariDataSource(PhysicalDataSourceProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        try {
            dataSource.setPoolName("sharding-" + properties.name());
            dataSource.setDriverClassName(properties.driverClassName());
            dataSource.setJdbcUrl(properties.jdbcUrl());
            dataSource.setUsername(properties.username());
            dataSource.setPassword(properties.password());
            return dataSource;
        } catch (RuntimeException failure) {
            dataSource.close();
            throw failure;
        }
    }

    @FunctionalInterface
    interface PhysicalDataSourceCreator {
        HikariDataSource create(PhysicalDataSourceProperties properties);
    }
}
