package top.egon.fable-web.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record AssignUserToClassRequest(@NotBlank String userId, @NotBlank String schoolClassId) {
}
