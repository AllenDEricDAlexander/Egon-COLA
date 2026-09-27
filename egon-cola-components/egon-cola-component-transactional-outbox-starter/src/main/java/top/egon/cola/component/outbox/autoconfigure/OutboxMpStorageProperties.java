package top.egon.cola.component.outbox.autoconfigure;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Duration;

/** Storage and maintenance settings for the managed MyBatis-Plus outbox. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OutboxMpStorageProperties {

    public static final String PREFIX = "egon.cola.component.transactional-outbox.storage.mp";

    @NotBlank
    @Builder.Default
    private String sqlSessionFactoryBeanName = "sqlSessionFactory";

    private boolean migrationMode;

    @Builder.Default
    private boolean autoInitializeDdl = true;

    @NotNull
    @Builder.Default
    private Duration migrationLockTimeout = Duration.ofSeconds(30);

    @NotBlank
    @Pattern(regexp = "db/egon-outbox-mp/manifest\\.json")
    @Builder.Default
    private String manifestResource = "db/egon-outbox-mp/manifest.json";

    public OutboxMpStorageProperties(String sqlSessionFactoryBeanName, boolean migrationMode,
                                     Duration migrationLockTimeout, String manifestResource) {
        this.sqlSessionFactoryBeanName = sqlSessionFactoryBeanName;
        this.migrationMode = migrationMode;
        this.migrationLockTimeout = migrationLockTimeout;
        this.manifestResource = manifestResource;
        this.autoInitializeDdl = true;
    }

    @AssertTrue(message = "Outbox MP migration lock timeout must be positive")
    public boolean isMigrationLockTimeoutPositive() {
        return migrationLockTimeout != null && !migrationLockTimeout.isNegative()
                && !migrationLockTimeout.isZero();
    }
}
