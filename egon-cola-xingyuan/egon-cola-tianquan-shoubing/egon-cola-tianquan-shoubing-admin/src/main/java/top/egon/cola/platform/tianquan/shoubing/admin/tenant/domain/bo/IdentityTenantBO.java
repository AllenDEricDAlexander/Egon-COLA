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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class IdentityTenantBO {

    private static final Pattern TENANT_CODE = Pattern.compile("^[a-z][a-z0-9-]{2,63}$");

    @NotBlank
    @Size(max = 64)
    private String identityTenantId;
    @NotBlank
    @Size(max = 64)
    private String tenantCode;
    @NotBlank
    @Size(max = 200)
    private String tenantName;
    @NotNull
    private Status status;
    @NotNull
    private String settings;
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

    public static IdentityTenantBO create(
            String identityTenantId,
            String tenantCode,
            String tenantName,
            String settings,
            String actor,
            Instant now
    ) {
        String id = canonicalTenantId(identityTenantId);
        String code = code(tenantCode);
        String name = name(tenantName);
        String json = settings(settings);
        Instant created = Objects.requireNonNull(now, "now");
        String createdActor = actor(actor);
        return IdentityTenantBO.builder()
                .identityTenantId(id)
                .tenantCode(code)
                .tenantName(name)
                .status(Status.INITIALIZING)
                .settings(json)
                .version(0L)
                .createdAt(created)
                .updatedAt(created)
                .createdBy(createdActor)
                .updatedBy(createdActor)
                .build();
    }

    public void update(
            String newTenantName,
            String newSettings,
            Status newStatus,
            long expectedVersion,
            String actor,
            Instant now
    ) {
        if (expectedVersion != version) {
            throw new IllegalStateException("tenant version conflict");
        }
        Objects.requireNonNull(newStatus, "status");
        if (status == Status.CLOSED && newStatus != Status.CLOSED) {
            throw new IllegalStateException("closed tenant cannot change status");
        }
        if (!legalTransition(status, newStatus)) {
            throw new IllegalStateException("invalid tenant status transition");
        }
        tenantName = newTenantName == null ? tenantName : name(newTenantName);
        settings = newSettings == null ? settings : settings(newSettings);
        status = newStatus;
        version = Math.addExact(version, 1L);
        updatedAt = Objects.requireNonNull(now, "now");
        updatedBy = actor(actor);
    }

    private static boolean legalTransition(Status current, Status next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case INITIALIZING -> next == Status.ACTIVE;
            case ACTIVE -> next == Status.SUSPENDED || next == Status.CLOSED;
            case SUSPENDED -> next == Status.ACTIVE || next == Status.CLOSED;
            case CLOSED -> false;
        };
    }

    private static String canonicalTenantId(String value) {
        String id = required(value, "identityTenantId");
        if (!id.matches("^[1-9][0-9]*$")) {
            throw new IllegalArgumentException("identityTenantId is invalid");
        }
        try {
            if (Long.parseLong(id) <= 0L) {
                throw new IllegalArgumentException("identityTenantId is invalid");
            }
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("identityTenantId is invalid", failure);
        }
        return id;
    }

    private static String code(String value) {
        String normalized = required(value, "tenantCode").toLowerCase(Locale.ROOT);
        if (!TENANT_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("tenantCode is invalid");
        }
        return normalized;
    }

    private static String name(String value) {
        String normalized = required(value, "tenantName");
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("tenantName is invalid");
        }
        return normalized;
    }

    private static String settings(String value) {
        String normalized = value == null || value.isBlank() ? "{}" : value.trim();
        if (!normalized.startsWith("{") || !normalized.endsWith("}")
                || normalized.getBytes(StandardCharsets.UTF_8).length > 65_536) {
            throw new IllegalArgumentException("tenant settings are invalid");
        }
        return normalized;
    }

    private static String actor(String value) {
        return required(value, "actor");
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    public enum Status {
        INITIALIZING,
        ACTIVE,
        SUSPENDED,
        CLOSED;

        @JsonValue
        public String jsonValue() {
            return name();
        }
    }
}
