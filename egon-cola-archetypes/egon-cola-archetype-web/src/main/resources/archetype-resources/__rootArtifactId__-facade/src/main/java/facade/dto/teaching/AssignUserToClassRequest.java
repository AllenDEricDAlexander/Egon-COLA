package ${package}.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record AssignUserToClassRequest(
        @NotBlank(message = "userId must not be blank")
        String userId,
        @NotBlank(message = "schoolClassId must not be blank")
        String schoolClassId
) {
}
