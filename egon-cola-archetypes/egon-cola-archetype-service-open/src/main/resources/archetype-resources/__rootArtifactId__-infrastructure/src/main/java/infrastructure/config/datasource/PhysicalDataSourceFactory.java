package ${package}.infrastructure.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Creates the named physical connection pools owned by the logical data source.
 */
public class PhysicalDataSourceFactory {

    private final PhysicalDataSourceCreator creator;

    public PhysicalDataSourceFactory() {
        this(PhysicalDataSourceFactory::createHikariDataSource);
    }

    PhysicalDataSourceFactory(PhysicalDataSourceCreator creator) {
        this.creator = creator;
    }

    public Map<String, DataSource> create(ShardingDataSourceProperties properties) {
        if (properties == null || properties.physicalDataSources().isEmpty()) {
            throw new IllegalArgumentException("physical data sources must not be empty");
        }
        Map<String, DataSource> result = new LinkedHashMap<>();
        try {
            for (ShardingDataSourceProperties.PhysicalDataSourceProperties source
                    : properties.physicalDataSources()) {
                validate(source);
                if (result.containsKey(source.name())) {
                    throw new IllegalArgumentException(
                            "duplicate physical data source name: " + source.name());
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
                    // Preserve the original startup failure while making a best-effort cleanup.
                }
            }
        }
    }

    private static HikariDataSource createHikariDataSource(
            ShardingDataSourceProperties.PhysicalDataSourceProperties properties) {
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

    private static void validate(
            ShardingDataSourceProperties.PhysicalDataSourceProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("physical data source must not be null");
        }
        requireText(properties.name(), "name", properties.name());
        requireText(properties.logicalName(), "logical name", properties.name());
        if (properties.role() == null) {
            throw invalid("role", properties.name());
        }
        requireText(properties.driverClassName(), "driver class name", properties.name());
        requireText(properties.jdbcUrl(), "JDBC URL", properties.name());
        requireText(properties.username(), "username", properties.name());
        if (properties.password() == null) {
            throw invalid("password", properties.name());
        }
    }

    private static void requireText(String value, String field, String sourceName) {
        if (value == null || value.isBlank()) {
            throw invalid(field, sourceName);
        }
    }

    private static IllegalArgumentException invalid(String field, String sourceName) {
        String safeName = sourceName == null || sourceName.isBlank() ? "<unnamed>" : sourceName;
        return new IllegalArgumentException(
                "physical data source " + safeName + " has invalid " + field);
    }

    @FunctionalInterface
    interface PhysicalDataSourceCreator {

        HikariDataSource create(
                ShardingDataSourceProperties.PhysicalDataSourceProperties properties);
    }
}
