package top.egon.cola.organization.facade.rpc;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Validates scalar identifiers after Protobuf presence has been decoded. */
public record RpcSchoolClassQuery(@NotNull @Positive Long gradeId, @NotNull @Positive Long schoolClassId) {
}
