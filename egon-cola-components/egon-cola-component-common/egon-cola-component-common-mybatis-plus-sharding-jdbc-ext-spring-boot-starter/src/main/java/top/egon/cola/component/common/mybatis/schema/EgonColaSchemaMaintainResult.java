package top.egon.cola.component.common.mybatis.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * One planned or applied TableInfo DDL action. DROP is never a legal action.
 */
public record EgonColaSchemaMaintainResult(
        @NotBlank String logicalTable,
        @NotBlank String action,
        @NotBlank String sql,
        @NotNull Instant appliedAt) {
}
