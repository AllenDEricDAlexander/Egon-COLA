package top.egon.cola.component.common.mybatis.sharding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-file ShardingSphere topology bound under the MyBatis-Plus starter prefix.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@Accessors(chain = true)
@Validated
@ConfigurationProperties(prefix = EgonColaShardingProperties.PREFIX)
public class EgonColaShardingProperties {

    public static final String PREFIX = "egon.cola.component.mybatis-plus.sharding";

    @Builder.Default
    @AssertTrue(message = "SHARDING_REQUIRED")
    private boolean enabled = true;

    @NonNull
    @NotNull
    private ModeEnum mode;

    @NonNull
    @NotNull
    private ConfigStyleEnum configStyle;

    @NotBlank
    @Builder.Default
    private String transactionDefaultType = "LOCAL";

    private String nativeRulesResource;

    @Valid
    @NotEmpty
    @Builder.Default
    private List<PhysicalDataSourceProperties> dataSources = new ArrayList<>();

    @Valid
    @NotNull
    @Builder.Default
    private Map<String, TableProperties> tables = new LinkedHashMap<>();

    public enum ModeEnum {
        SHARDING,
        SHARDING_READWRITE
    }

    public enum ConfigStyleEnum {
        STRATEGY,
        NATIVE
    }

    public enum DataSourceRoleEnum {
        PRIMARY,
        REPLICA
    }

    public record PhysicalDataSourceProperties(
            @NotBlank @Pattern(regexp = "[a-zA-Z_][a-zA-Z0-9_-]*") String name,
            @NotBlank @Pattern(regexp = "[a-zA-Z_][a-zA-Z0-9_-]*") String logicalName,
            @NotNull DataSourceRoleEnum role,
            @NotBlank @Pattern(regexp = "org\\.postgresql\\.Driver") String driverClassName,
            @NotBlank @Pattern(regexp = "jdbc:postgresql:.*") String jdbcUrl,
            @NotBlank String username,
            @NotNull String password) {

        @Override
        public String toString() {
            return "PhysicalDataSourceProperties[name=" + name
                    + ", logicalName=" + logicalName
                    + ", role=" + role
                    + ", connection=<redacted>]";
        }
    }

    public record TableProperties(
            @NotBlank String type,
            String dataSource,
            String shardingColumn,
            List<String> tableColumns,
            String rootKeyName) {
    }
}
