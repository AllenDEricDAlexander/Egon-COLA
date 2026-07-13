package ${package}.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignUserToClassDTO(@NotBlank String userId, @NotBlank String schoolClassId) {
}
