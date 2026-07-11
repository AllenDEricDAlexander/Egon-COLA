package ${package}.adapter.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record AssignUserToClassRequest(@NotBlank String userId) {
}
