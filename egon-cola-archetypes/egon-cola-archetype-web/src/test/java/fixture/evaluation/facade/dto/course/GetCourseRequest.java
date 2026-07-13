package fixture.evaluation.facade.dto.course;

import java.io.Serializable;

public record GetCourseRequest(String courseId) implements Serializable {
}
