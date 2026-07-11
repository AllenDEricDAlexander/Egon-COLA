package ${package}.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record AssignUserToClassDTO(@NotBlank String userId, @NotBlank String schoolClassId) {
}
