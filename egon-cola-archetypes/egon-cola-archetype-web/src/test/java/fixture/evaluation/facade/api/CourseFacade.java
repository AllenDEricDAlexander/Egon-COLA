package fixture.evaluation.facade.api;

import fixture.evaluation.facade.dto.SingleResponse;
import fixture.evaluation.facade.dto.course.CourseResponse;
import fixture.evaluation.facade.dto.course.GetCourseRequest;

public interface CourseFacade {

    SingleResponse<CourseResponse> getCourse(GetCourseRequest request);
}
