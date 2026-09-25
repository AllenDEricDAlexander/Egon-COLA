package top.egon.cola.component.outbox.migration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.core.pojo.BasePojo;

/** Safe summary of an outbox legacy migration and its full verification pass. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OutboxMigrationResult implements BasePojo {

    @NotBlank
    @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}")
    private String sourceSchema;

    @NotBlank
    @Pattern(regexp = "egon_outbox")
    @Builder.Default
    private String targetSchema = "egon_outbox";

    @NotNull
    @PositiveOrZero
    private Long sourceCount;

    @NotNull
    @PositiveOrZero
    private Long targetCount;

    @NotNull
    @PositiveOrZero
    private Long copiedCount;

    @NotNull
    @PositiveOrZero
    private Long matchedCount;

    @NotNull
    @PositiveOrZero
    private Long differenceCount;

    @Positive
    private Long firstDifferenceId;

    private boolean verified;

    @AssertTrue(message = "Verified outbox migration must have equal counts and no differences")
    public boolean isVerifiedAccurate() {
        return !verified || sourceCount != null && sourceCount.equals(targetCount)
                && differenceCount != null && differenceCount == 0 && firstDifferenceId == null;
    }
}
