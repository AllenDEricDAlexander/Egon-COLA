package top.egon.cola.component.common.mybatis.routing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.List;
import java.util.Objects;

/**
 * Bound routing keys, independent of SQL parser and ShardingSphere callback types.
 */
public record EgonColaRouteQuery(
        @NotBlank String logicalTable,
        @NotNull Long tenantId,
        @NotNull @Size(max = 10000) List<@NotNull @Positive Long> secondaryValues,
        @NotNull OperationEnum operation,
        boolean rangeRequested) {

    public EgonColaRouteQuery {
        EgonColaPhysicalTargetBO.requireIdentifier(logicalTable, "logical table");
        Objects.requireNonNull(secondaryValues, "secondaryValues");
        if (secondaryValues.size() > 10000) {
            throw new IllegalArgumentException("Too many secondary keys");
        }
        for (Long value : secondaryValues) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException("Secondary keys must be positive Long values");
            }
        }
        secondaryValues = rangeRequested ? List.of() : secondaryValues.stream().distinct().sorted().toList();
    }

    public enum OperationEnum implements EgonEnum {
        QUERY(0, "QUERY"),
        COMMAND(1, "COMMAND"),
        /** Candidate computation is internal to an SS callback and never authorizes a SQL command. */
        ROUTE_CANDIDATES(2, "ROUTE_CANDIDATES");

        private final int code;
        private final String message;

        OperationEnum(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
