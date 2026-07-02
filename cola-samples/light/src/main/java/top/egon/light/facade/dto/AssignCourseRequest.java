package top.egon.light.facade.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignCourseRequest(
        @NotBlank String studentId,
        @NotBlank String courseId
) {
}
