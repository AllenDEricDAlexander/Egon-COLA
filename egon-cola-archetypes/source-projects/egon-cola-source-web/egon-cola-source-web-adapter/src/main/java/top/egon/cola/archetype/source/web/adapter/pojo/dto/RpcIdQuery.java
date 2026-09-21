package top.egon.cola.archetype.source.web.adapter.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Validates scalar identifiers after Protobuf presence has been decoded. */
public record RpcIdQuery(@NotNull @Positive Long id) {
}
