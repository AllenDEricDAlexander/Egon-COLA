package fixture.evaluation.facade.course.dto;

import java.io.Serializable;

public record GetCourseRequest(String courseId) implements Serializable {
}
