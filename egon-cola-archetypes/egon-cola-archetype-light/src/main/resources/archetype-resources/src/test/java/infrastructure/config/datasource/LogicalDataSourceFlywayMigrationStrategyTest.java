package ${package}.infrastructure.config.datasource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LogicalDataSourceFlywayMigrationStrategyTest {

    @Test
    void shouldNotMigrateTheLogicalDataSource() {
        Flyway flyway = mock(Flyway.class);

        new LogicalDataSourceFlywayMigrationStrategy().migrate(flyway);

        verifyNoInteractions(flyway);
    }
}
