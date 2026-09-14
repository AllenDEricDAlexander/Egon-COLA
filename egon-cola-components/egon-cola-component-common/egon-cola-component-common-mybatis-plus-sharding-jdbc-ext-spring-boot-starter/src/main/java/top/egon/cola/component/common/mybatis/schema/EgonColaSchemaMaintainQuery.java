package top.egon.cola.component.common.mybatis.schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * One schema-maintenance intent over scanned models and PRIMARY physical groups.
 */
public record EgonColaSchemaMaintainQuery(
        @NotEmpty List<@NotNull String> modelPackages,
        @NotEmpty Map<@NotNull String, @NotNull EgonColaRoutingProfileBO> profiles,
        @NotEmpty Map<@NotNull String, @NotNull DataSource> primaryDataSources,
        boolean dropExtraColumns) {
}
