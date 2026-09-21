package top.egon.cola.archetype.source.lightopen.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateUserDTO(
        @NotBlank String externalId,
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank String operatorId,
        @NotBlank String requestId) implements BasePojo {
}
