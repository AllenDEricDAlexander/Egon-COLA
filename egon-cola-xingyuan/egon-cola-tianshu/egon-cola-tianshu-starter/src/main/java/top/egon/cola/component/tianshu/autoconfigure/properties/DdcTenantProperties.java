package top.egon.cola.component.tianshu.autoconfigure.properties;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@ConfigurationProperties(prefix = "egon.cola.component.tianshu")
public class DdcTenantProperties {

    @Positive
    private Long tenantId;

    @Builder.Default
    private DdcRedisAuthDTO redis = new DdcRedisAuthDTO();

    public long requireTenantId() {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException(
                    "egon.cola.component.tianshu.tenant-id must be a positive Long");
        }
        return tenantId;
    }

    public String requireRedisUsername() {
        String username = redis == null ? null : redis.getUsername();
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                    "egon.cola.component.tianshu.redis.username is required when Redis is enabled");
        }
        return username.trim();
    }
}
