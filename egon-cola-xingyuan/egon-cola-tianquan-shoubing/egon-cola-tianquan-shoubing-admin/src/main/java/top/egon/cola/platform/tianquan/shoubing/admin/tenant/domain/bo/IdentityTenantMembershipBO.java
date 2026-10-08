package top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class IdentityTenantMembershipBO {

    @NotBlank
    @Size(max = 64)
    private String membershipId;
    @NotBlank
    @Size(max = 64)
    private String identityTenantId;
    @NotBlank
    @Size(max = 64)
    private String identitySub;
    @NotNull
    private Status status;
    @PositiveOrZero
    private long version;
    @NotNull
    private Instant createdAt;
    @NotNull
    private Instant updatedAt;
    @NotBlank
    @Size(max = 128)
    private String createdBy;
    @NotBlank
    @Size(max = 128)
    private String updatedBy;

    public static IdentityTenantMembershipBO create(
            String membershipId,
            String identityTenantId,
            String identitySub,
            Status status,
            String actor,
            Instant now
    ) {
        String createdActor = required(actor, "actor");
        Instant created = Objects.requireNonNull(now, "now");
        return IdentityTenantMembershipBO.builder()
                .membershipId(required(membershipId, "membershipId"))
                .identityTenantId(positiveTenantId(identityTenantId))
                .identitySub(required(identitySub, "identitySub"))
                .status(Objects.requireNonNull(status, "status"))
                .version(0L)
                .createdAt(created)
                .updatedAt(created)
                .createdBy(createdActor)
                .updatedBy(createdActor)
                .build();
    }

    public void update(Status newStatus, long expectedVersion, String actor, Instant now) {
        if (expectedVersion != version) {
            throw new IllegalStateException("membership version conflict");
        }
        status = Objects.requireNonNull(newStatus, "status");
        version = Math.addExact(version, 1L);
        updatedAt = Objects.requireNonNull(now, "now");
        updatedBy = required(actor, "actor");
    }

    private static String positiveTenantId(String value) {
        String id = required(value, "identityTenantId");
        if (!id.matches("^[1-9][0-9]*$") || id.length() > 19) {
            throw new IllegalArgumentException("identityTenantId is invalid");
        }
        try {
            Long.parseLong(id);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("identityTenantId is invalid", failure);
        }
        return id;
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    public enum Status {
        ACTIVE,
        DISABLED;

        @JsonValue
        public String jsonValue() {
            return name();
        }
    }
}
