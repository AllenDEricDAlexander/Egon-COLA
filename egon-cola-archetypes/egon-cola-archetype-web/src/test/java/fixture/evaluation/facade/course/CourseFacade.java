package fixture.evaluation.facade.course;

import fixture.evaluation.facade.dto.SingleResponse;
import fixture.evaluation.facade.course.dto.CourseResponse;
import fixture.evaluation.facade.course.dto.GetCourseRequest;

public interface CourseFacade {

    SingleResponse<CourseResponse> getCourse(GetCourseRequest request);
}
