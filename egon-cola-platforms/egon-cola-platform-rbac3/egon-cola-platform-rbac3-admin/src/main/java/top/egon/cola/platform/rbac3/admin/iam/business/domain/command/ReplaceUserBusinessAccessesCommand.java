package top.egon.cola.platform.rbac3.admin.iam.business.domain.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.enums.UserBusinessAccessStatusEnum;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Replaces only the target user's MANUAL Business grants. */
public record ReplaceUserBusinessAccessesCommand(
        String commandId,
        @Valid List<Item> items) {

    public ReplaceUserBusinessAccessesCommand(List<Item> items) {
        this(UUID.randomUUID().toString(), items);
    }

    public ReplaceUserBusinessAccessesCommand {
        commandId = required(commandId, "commandId");
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }

    /** One desired MANUAL Business grant fact. */
    public record Item(
            @NotBlank String ddcBusinessId,
            @NotNull UserBusinessAccessStatusEnum status,
            @NotNull Instant validFrom,
            Instant validTo,
            String reason,
            String ticketNo,
            @PositiveOrZero long expectedVersion) {

        public Item {
            ddcBusinessId = required(ddcBusinessId, "ddcBusinessId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(validFrom, "validFrom");
            if (validTo != null && !validTo.isAfter(validFrom)) {
                throw new IllegalArgumentException("validTo must be after validFrom");
            }
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
