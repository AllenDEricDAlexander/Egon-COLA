package top.egon.cola.component.tianshu.admin.model.query;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DdcAiIdQuery(
        @NotNull @Positive Long id) {
}
