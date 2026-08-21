package top.egon.cola.component.common.mybatis.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
    private TenantId tenantId = new TenantId();

    @Valid
    private Audit audit = new Audit();

    @Valid
    private Pagination pagination = new Pagination();

    @Valid
    private Batch batch = new Batch();

    @Valid
    private Toggle blockAttack = new Toggle();

    @Valid
    private Toggle optimisticLocker = new Toggle();

    @Valid
    private Toggle metaFill = new Toggle();

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
        @Max(100_000)
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
        @Max(1_000_000)
        private int defaultSize = 1_000;

        @Min(1)
        @Max(1_000_000)
        private int maxChunkSize = 1_000;

        @Min(1)
        @Max(1_000_000)
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

    public static class Toggle {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
