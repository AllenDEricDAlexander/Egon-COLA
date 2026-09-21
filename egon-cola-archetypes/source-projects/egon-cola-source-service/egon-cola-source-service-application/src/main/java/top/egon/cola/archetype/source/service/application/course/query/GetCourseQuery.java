package top.egon.cola.archetype.source.service.application.course.query;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetCourseQuery(@NotNull @Positive Long courseId) {
}
