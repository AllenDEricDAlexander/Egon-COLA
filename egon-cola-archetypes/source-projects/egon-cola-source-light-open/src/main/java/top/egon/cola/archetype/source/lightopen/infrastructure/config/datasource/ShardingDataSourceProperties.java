package top.egon.cola.archetype.source.lightopen.infrastructure.config.datasource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;

import java.util.List;

/** Physical topology, immutable routing addresses and explicitly owned PostgreSQL DDL targets. */
public record ShardingDataSourceProperties(
        @NotBlank String config,
        @NotNull @Valid ShardingRoutingProperties routing,
        @NotEmpty List<@NotNull @Valid PhysicalDataSourceProperties> physicalDataSources,
        @NotNull @Valid ShardingDdlProperties ddl) {

    public ShardingDataSourceProperties {
        physicalDataSources = physicalDataSources == null ? List.of() : List.copyOf(physicalDataSources);
        ddl = ddl == null ? new ShardingDdlProperties(List.of()) : ddl;
    }

    public enum DataSourceRole { PRIMARY, REPLICA }

    public record PhysicalDataSourceProperties(
            @NotBlank @Pattern(regexp = "[a-zA-Z_][a-zA-Z0-9_-]*") String name,
            @NotBlank @Pattern(regexp = "[a-zA-Z_][a-zA-Z0-9_-]*") String logicalName,
            @NotNull DataSourceRole role,
            @NotBlank @Pattern(regexp = "org\\.postgresql\\.Driver") String driverClassName,
            @NotBlank @Pattern(regexp = "jdbc:postgresql:.*") String jdbcUrl,
            @NotBlank String username,
            @NotNull String password) {
        @Override
        public String toString() {
            return "PhysicalDataSourceProperties[name=" + name + ", logicalName=" + logicalName + ", role=" + role + ", connection=<redacted>]";
        }
    }

    public record ShardingRoutingProperties(@Min(2) int nodeCount, @NotBlank String nodeMap) {}

    public record DdlTargetProperties(
            @NotBlank String dataSourceName,
            @NotBlank @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String schema,
            @NotNull EgonColaDdlTargetBO.RoleEnum role,
            @NotBlank String manifest) {}

    public record ShardingDdlProperties(@NotEmpty List<@NotNull @Valid DdlTargetProperties> targets) {
        public ShardingDdlProperties { targets = targets == null ? List.of() : List.copyOf(targets); }
    }
}
