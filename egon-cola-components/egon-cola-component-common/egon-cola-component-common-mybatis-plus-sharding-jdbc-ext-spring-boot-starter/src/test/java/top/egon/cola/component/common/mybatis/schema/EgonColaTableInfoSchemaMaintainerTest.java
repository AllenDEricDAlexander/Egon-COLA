package top.egon.cola.component.common.mybatis.schema;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategyNodes;
import top.egon.cola.component.common.mybatis.schema.support.PoMissingTenant;
import top.egon.cola.component.common.mybatis.schema.support.time.TimeTypesPO;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EgonColaTableInfoSchemaMaintainerTest {

    private final EgonColaTableInfoSchemaMaintainer maintainer = new EgonColaTableInfoSchemaMaintainer(
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()),
            Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void rejects_model_without_tenant_id() {
        assertThatThrownBy(() -> maintainer.maintain(query(PoMissingTenant.class.getPackageName(), "missing_tenant")))
                .isInstanceOf(EgonColaMybatisPlusConfigurationException.class)
                .hasMessageContaining("TENANT_ID_REQUIRED");
    }

    @Test
    void does_not_emit_drop_column() {
        List<EgonColaSchemaMaintainResult> results = maintainer.maintain(
                query(TestBusinessModel.class.getPackageName(), "test_business_record"));
        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(result -> {
            assertThat(result.sql().toUpperCase()).doesNotContain("DROP");
            assertThat(result.appliedAt()).isEqualTo(Instant.parse("2026-09-15T00:00:00Z"));
        });
    }

    @Test
    void temporalProposalKeepsDateTimeSemanticsAndNeverExecutesTheDataSource() {
        EgonColaSchemaMaintainQuery query = query(TimeTypesPO.class.getPackageName(), "time_types");
        List<EgonColaSchemaMaintainResult> results = maintainer.maintain(query);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).sql()).contains(
                "\"occurred_at\" TIMESTAMPTZ(6)", "\"offset_at\" TIMESTAMPTZ(6)",
                "\"local_at\" TIMESTAMP(6) WITHOUT TIME ZONE", "\"pure_date\" DATE",
                "\"local_time\" TIME(6) WITHOUT TIME ZONE", "\"deleted_at\" TIMESTAMP(6) WITHOUT TIME ZONE",
                "\"create_time\" TIMESTAMPTZ(6)", "\"update_time\" TIMESTAMPTZ(6)");
        query.primaryDataSources().values().forEach(dataSource -> verifyNoInteractions(dataSource));
    }

    @Test
    void unsupportedTemporalTypesAndArraysCannotFallBackToText() {
        for (String table : List.of("unsupported_zoned_time", "unsupported_offset_time", "unsupported_duration", "unsupported_time_array")) {
            EgonColaSchemaMaintainQuery query = query(TimeTypesPO.class.getPackageName(), table);
            assertThatThrownBy(() -> maintainer.maintain(query))
                    .isInstanceOf(EgonColaMybatisPlusConfigurationException.class).hasMessageContaining("TIME_TYPE_UNSUPPORTED");
            query.primaryDataSources().values().forEach(dataSource -> verifyNoInteractions(dataSource));
        }
    }

    @Test
    void rejectedDropIntentDoesNotContactTheDataSource() {
        EgonColaSchemaMaintainQuery normal = query(TimeTypesPO.class.getPackageName(), "time_types");
        EgonColaSchemaMaintainQuery forbidden = new EgonColaSchemaMaintainQuery(
                normal.modelPackages(), normal.profiles(), normal.primaryDataSources(), true);
        assertThatThrownBy(() -> maintainer.maintain(forbidden)).hasMessageContaining("DROP_FORBIDDEN");
        forbidden.primaryDataSources().values().forEach(dataSource -> verifyNoInteractions(dataSource));
    }

    private static EgonColaSchemaMaintainQuery query(String modelPackage, String logicalTable) {
        EgonColaRoutingProfileBO profile = EgonColaShardingStrategyNodes.simple(
                logicalTable,
                TableKindEnum.SINGLE,
                List.of(new EgonColaPhysicalTargetBO("master_data", "public", logicalTable)));
        return new EgonColaSchemaMaintainQuery(
                List.of(modelPackage),
                Map.of(logicalTable, profile),
                Map.of("master_data", mock(DataSource.class)),
                false);
    }
}
