package top.egon.cola.evaluation.facade.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

public record ScheduleCourseRequest(
        @NotBlank String courseId,
        @NotBlank String classId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) implements Serializable {
}
