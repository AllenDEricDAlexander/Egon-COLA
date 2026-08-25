package top.egon.cola.evaluation.facade.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record GetCourseRequest(@NotNull @Positive Long courseId) implements Serializable {
}
