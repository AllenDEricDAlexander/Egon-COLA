package top.egon.cola.platform.rbac3.admin.integration.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * Owns the independent RBAC3 and Transactional Outbox migration histories.
 */
@Configuration(proxyBeanMethods = false)
public class Rbac3FlywayConfiguration {

    public static final String RBAC3_FLYWAY = "rbac3Flyway";
    public static final String OUTBOX_FLYWAY = "outboxFlyway";
    public static final String RBAC3_INITIALIZER = "rbac3FlywayInitializer";
    public static final String OUTBOX_INITIALIZER = "outboxFlywayInitializer";

    @Bean(RBAC3_FLYWAY)
    Flyway rbac3Flyway(DataSource dataSource) {
        return buildRbac3Flyway(dataSource);
    }

    @Bean(OUTBOX_FLYWAY)
    Flyway outboxFlyway(DataSource dataSource) {
        return buildOutboxFlyway(dataSource);
    }

    @Bean(RBAC3_INITIALIZER)
    FlywayMigrationInitializer rbac3FlywayInitializer(
            @Qualifier(RBAC3_FLYWAY) Flyway rbac3Flyway) {
        return new FlywayMigrationInitializer(rbac3Flyway);
    }

    @Bean(OUTBOX_INITIALIZER)
    FlywayMigrationInitializer outboxFlywayInitializer(
            @Qualifier(OUTBOX_FLYWAY) Flyway outboxFlyway) {
        return new FlywayMigrationInitializer(outboxFlyway);
    }

    /**
     * Forces JPA validation and Outbox schema validation behind both migrations.
     */
    @Bean
    static BeanFactoryPostProcessor rbac3MigrationDependencies() {
        return beanFactory -> {
            dependOn(beanFactory, "entityManagerFactory");
            dependOn(beanFactory, "outboxSchemaValidator");
        };
    }

    public static Flyway buildRbac3Flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .table("flyway_schema_history_rbac3")
                .validateMigrationNaming(true)
                .load();
    }

    public static Flyway buildOutboxFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/transactional-outbox/postgresql")
                .table("flyway_schema_history_outbox")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .validateMigrationNaming(true)
                .load();
    }

    private static void dependOn(
            org.springframework.beans.factory.config.ConfigurableListableBeanFactory factory,
            String beanName) {
        if (!factory.containsBeanDefinition(beanName)) {
            return;
        }
        BeanDefinition definition = factory.getBeanDefinition(beanName);
        var dependencies = new LinkedHashSet<String>();
        if (definition.getDependsOn() != null) {
            dependencies.addAll(Arrays.asList(definition.getDependsOn()));
        }
        dependencies.add(RBAC3_INITIALIZER);
        dependencies.add(OUTBOX_INITIALIZER);
        definition.setDependsOn(dependencies.toArray(String[]::new));
    }
}
