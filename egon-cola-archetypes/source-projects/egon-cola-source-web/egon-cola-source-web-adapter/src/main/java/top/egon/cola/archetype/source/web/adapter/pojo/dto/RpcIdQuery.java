package top.egon.cola.archetype.source.web.adapter.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.component.common.core.pojo.BasePojo;

/** Validates scalar identifiers after Protobuf presence has been decoded. */
public record RpcIdQuery(@NotNull @Positive Long id) implements BasePojo {
}
