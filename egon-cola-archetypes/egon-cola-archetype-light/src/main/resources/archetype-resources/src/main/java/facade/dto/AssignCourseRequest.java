package ${package}.facade.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignCourseRequest(
        @NotBlank(message = "studentId must not be blank")
        String studentId,
        @NotBlank(message = "courseId must not be blank")
        String courseId
) {
}
