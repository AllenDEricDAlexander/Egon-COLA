package ${package}.adapter.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignUserToClassRequest(@NotBlank String userId) {
}
