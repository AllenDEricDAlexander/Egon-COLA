package top.egon.cola.component.common.mybatis.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Technical policies for the Egon COLA MyBatis-Plus starter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@ConfigurationProperties(prefix = EgonColaMybatisPlusProperties.PREFIX)
public class EgonColaMybatisPlusProperties {

    public static final String PREFIX = "egon.cola.component.mybatis-plus";

    @Builder.Default
    private boolean enabled = true;

    @Valid
    @NotNull
    @Builder.Default
    private TenantId tenantId = new TenantId();

    @Valid
    @NotNull
    @Builder.Default
    private Audit audit = new Audit();

    @Valid
    @NotNull
    @Builder.Default
    private Pagination pagination = new Pagination();

    @Valid
    @NotNull
    @Builder.Default
    private Batch batch = new Batch();

    @Valid
    @NotNull
    @Builder.Default
    private Toggle blockAttack = new Toggle();

    @Valid
    @NotNull
    @Builder.Default
    private Toggle optimisticLocker = new Toggle();

    @Valid
    @NotNull
    @Builder.Default
    private Ddl ddl = new Ddl();
    @Valid
    @NotNull
    @Builder.Default
    private DynamicTableName dynamicTableName = new DynamicTableName();
    @Valid
    @NotNull
    @Builder.Default
    private Toggle illegalSql = new Toggle(false);
    @Valid
    @NotNull
    @Builder.Default
    private LocalWriteGuard localWriteGuard = new LocalWriteGuard();
    @Valid
    @NotNull
    @Builder.Default
    private LifecycleProperties lifecycle = new LifecycleProperties();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class LifecycleProperties {

        private static final String MAPPER_STATEMENT_ID_PATTERN =
                "(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)+[A-Za-z_$][A-Za-z0-9_$]*";

        @NotNull
        @Builder.Default
        private Set<@NotBlank @Pattern(regexp = MAPPER_STATEMENT_ID_PATTERN) String> historyReadStatements = new LinkedHashSet<>();

        @NotNull
        @Builder.Default
        private Set<@NotBlank @Pattern(regexp = MAPPER_STATEMENT_ID_PATTERN) String> restoreStatements = new LinkedHashSet<>();

        public Set<String> getHistoryReadStatements() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(historyReadStatements));
        }

        public void setHistoryReadStatements(Set<String> statementIds) {
            historyReadStatements = statementIds == null
                    ? new LinkedHashSet<>() : new LinkedHashSet<>(statementIds);
        }

        public Set<String> getRestoreStatements() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(restoreStatements));
        }

        public void setRestoreStatements(Set<String> statementIds) {
            restoreStatements = statementIds == null
                    ? new LinkedHashSet<>() : new LinkedHashSet<>(statementIds);
        }
    }

    @Data
    public static class TenantId {

        @NotBlank
        private String mdcKey = EgonColaTenantIdProvider.DEFAULT_MDC_KEY;
        private Set<String> ignoredTables = new LinkedHashSet<>();

        public boolean ignores(String table) {
            String expected = normalizedTable(table);
            return ignoredTables.stream().filter(java.util.Objects::nonNull)
                    .map(TenantId::normalizedTable).anyMatch(expected::equals);
        }

        private static String normalizedTable(String table) {
            String value = table.trim().replace("\"", "").replace("`", "");
            return value.substring(value.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        }

        public void setIgnoredTables(Set<String> ignoredTables) {
            this.ignoredTables = ignoredTables == null
                    ? new LinkedHashSet<>() : new LinkedHashSet<>(ignoredTables);
        }
    }

    @Data
    public static class Audit {
        @NotBlank
        private String userIdMdcKey = "userId";
    }

    @Data
    public static class Pagination {

        private boolean enabled = true;

        @Min(1)
        @Max(500)
        private int maxPageSize = 500;

        private boolean overflow;
    }

    @Data
    public static class Batch {

        @Min(1)
        @Max(1_000)
        private int defaultSize = 1_000;

        @Min(1)
        @Max(1_000)
        private int maxChunkSize = 1_000;

        @Min(1)
        @Max(10_000)
        private int maxCollectionSize = 10_000;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Ddl extends Toggle {
        @NotNull
        private Duration lockTimeout = Duration.ofSeconds(30);
        @NotNull
        private Duration statementTimeout = Duration.ofSeconds(300);
        @NotNull
        private Duration topologyReadyTimeout = Duration.ofSeconds(60);

        public Ddl() {
            super(false);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DynamicTableName extends Toggle {
        @NotNull
        private Map<@Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String,
                @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String> tables = new LinkedHashMap<>();

        public DynamicTableName() {
            super(false);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class LocalWriteGuard extends Toggle {
        @NotNull
        private Map<@NotBlank String, @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String> allowedRootStatements = new LinkedHashMap<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Toggle {
        private boolean enabled = true;
    }
}
