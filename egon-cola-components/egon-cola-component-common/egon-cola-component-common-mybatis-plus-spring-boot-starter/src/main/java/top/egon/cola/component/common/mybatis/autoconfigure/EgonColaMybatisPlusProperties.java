package top.egon.cola.component.common.mybatis.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Technical policies for the Egon COLA MyBatis-Plus starter.
 */
@Validated
@ConfigurationProperties(prefix = EgonColaMybatisPlusProperties.PREFIX)
public class EgonColaMybatisPlusProperties {

    public static final String PREFIX = "egon.cola.component.mybatis-plus";

    private boolean enabled = true;

    @Valid
    @NotNull
    private TenantId tenantId = new TenantId();

    @Valid
    @NotNull
    private Audit audit = new Audit();

    @Valid
    @NotNull
    private Pagination pagination = new Pagination();

    @Valid
    @NotNull
    private Batch batch = new Batch();

    @Valid
    @NotNull
    private Toggle blockAttack = new Toggle();

    @Valid
    @NotNull
    private Toggle optimisticLocker = new Toggle();

    @Valid
    @NotNull
    private Toggle metaFill = new Toggle();

    @Valid @NotNull
    private Ddl ddl = new Ddl();
    @Valid @NotNull
    private DynamicTableName dynamicTableName = new DynamicTableName();
    @Valid @NotNull
    private Toggle dataChangeRecorder = new Toggle(false);
    @Valid @NotNull
    private Toggle illegalSql = new Toggle(false);
    @Valid @NotNull
    private LocalWriteGuard localWriteGuard = new LocalWriteGuard();

    public Ddl getDdl() { return ddl; }
    public void setDdl(Ddl ddl) { this.ddl = ddl; }
    public DynamicTableName getDynamicTableName() { return dynamicTableName; }
    public void setDynamicTableName(DynamicTableName dynamicTableName) { this.dynamicTableName = dynamicTableName; }
    public Toggle getDataChangeRecorder() { return dataChangeRecorder; }
    public void setDataChangeRecorder(Toggle dataChangeRecorder) { this.dataChangeRecorder = dataChangeRecorder; }
    public Toggle getIllegalSql() { return illegalSql; }
    public void setIllegalSql(Toggle illegalSql) { this.illegalSql = illegalSql; }
    public LocalWriteGuard getLocalWriteGuard() { return localWriteGuard; }
    public void setLocalWriteGuard(LocalWriteGuard localWriteGuard) { this.localWriteGuard = localWriteGuard; }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Toggle getBlockAttack() {
        return blockAttack;
    }

    public void setBlockAttack(Toggle blockAttack) {
        this.blockAttack = blockAttack;
    }

    public Toggle getOptimisticLocker() {
        return optimisticLocker;
    }

    public void setOptimisticLocker(Toggle optimisticLocker) {
        this.optimisticLocker = optimisticLocker;
    }

    public Toggle getMetaFill() {
        return metaFill;
    }

    public void setMetaFill(Toggle metaFill) {
        this.metaFill = metaFill;
    }

    public static class TenantId {

        @NotBlank
        private String mdcKey = "tenantId";
        private Set<String> ignoredTables = new LinkedHashSet<>();

        public String getMdcKey() {
            return mdcKey;
        }

        public void setMdcKey(String mdcKey) {
            this.mdcKey = mdcKey;
        }

        public Set<String> getIgnoredTables() {
            return ignoredTables;
        }

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

    public static class Audit {

        @NotBlank
        private String userIdMdcKey = "userId";

        public String getUserIdMdcKey() {
            return userIdMdcKey;
        }

        public void setUserIdMdcKey(String userIdMdcKey) {
            this.userIdMdcKey = userIdMdcKey;
        }
    }

    public static class Pagination {

        private boolean enabled = true;

        @Min(1)
        @Max(500)
        private int maxPageSize = 500;

        private boolean overflow;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }

        public boolean isOverflow() {
            return overflow;
        }

        public void setOverflow(boolean overflow) {
            this.overflow = overflow;
        }
    }

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

        public int getDefaultSize() {
            return defaultSize;
        }

        public void setDefaultSize(int defaultSize) {
            this.defaultSize = defaultSize;
        }

        public int getMaxChunkSize() {
            return maxChunkSize;
        }

        public void setMaxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
        }

        public int getMaxCollectionSize() {
            return maxCollectionSize;
        }

        public void setMaxCollectionSize(int maxCollectionSize) {
            this.maxCollectionSize = maxCollectionSize;
        }
    }

    public static class Ddl extends Toggle {
        @NotNull private Duration lockTimeout = Duration.ofSeconds(30);
        @NotNull private Duration statementTimeout = Duration.ofSeconds(300);
        @NotNull private Duration topologyReadyTimeout = Duration.ofSeconds(60);

        public Ddl() { super(false); }
        public Duration getLockTimeout() { return lockTimeout; }
        public void setLockTimeout(Duration value) { lockTimeout = value; }
        public Duration getStatementTimeout() { return statementTimeout; }
        public void setStatementTimeout(Duration value) { statementTimeout = value; }
        public Duration getTopologyReadyTimeout() { return topologyReadyTimeout; }
        public void setTopologyReadyTimeout(Duration value) { topologyReadyTimeout = value; }
    }

    public static class DynamicTableName extends Toggle {
        @NotNull
        private Map<@Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String,
                @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String> tables = new LinkedHashMap<>();

        public DynamicTableName() { super(false); }
        public Map<String, String> getTables() { return tables; }
        public void setTables(Map<String, String> tables) { this.tables = new LinkedHashMap<>(tables); }
    }

    public static class LocalWriteGuard extends Toggle {
        @NotNull
        private Map<@NotBlank String, @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String> allowedRootStatements = new LinkedHashMap<>();

        public Map<String, String> getAllowedRootStatements() { return allowedRootStatements; }
        public void setAllowedRootStatements(Map<String, String> statements) { allowedRootStatements = new LinkedHashMap<>(statements); }
    }

    public static class Toggle {

        public Toggle() { }
        public Toggle(boolean enabled) { this.enabled = enabled; }

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
