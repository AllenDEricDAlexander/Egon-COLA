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
