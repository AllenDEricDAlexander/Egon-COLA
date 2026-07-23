package top.egon.cola.evaluation.facade.course.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record GetCourseRequest(@NotBlank String courseId) implements Serializable {
}
