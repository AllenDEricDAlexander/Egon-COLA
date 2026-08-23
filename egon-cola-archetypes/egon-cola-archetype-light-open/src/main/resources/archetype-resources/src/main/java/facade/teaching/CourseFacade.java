package ${package}.facade.teaching;

import ${package}.facade.teaching.dto.CourseDTO;
import ${package}.facade.teaching.dto.CreateCourseDTO;

public interface CourseFacade {
    CourseDTO createCourse(CreateCourseDTO request);

    CourseDTO getCourse(String courseId);
}
