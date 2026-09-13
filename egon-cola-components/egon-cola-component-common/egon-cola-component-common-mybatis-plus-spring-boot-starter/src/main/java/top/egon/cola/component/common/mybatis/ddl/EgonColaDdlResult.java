package top.egon.cola.component.common.mybatis.ddl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Duration;
import java.util.Objects;

/** One committed or previously verified version; this is not a distributed transaction receipt. */
public record EgonColaDdlResult(
        @NotBlank String alias,
        @NotBlank String schema,
        @NotBlank String version,
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String checksum,
        @NotNull StatusEnum status,
        @NotNull Duration elapsed) {

    public EgonColaDdlResult {
        Objects.requireNonNull(status, "status");
        if (elapsed == null || elapsed.isNegative()) {
            throw new IllegalArgumentException("DDL elapsed duration must be nonnegative");
        }
    }

    public enum StatusEnum {
        APPLIED, SKIPPED
    }
}
