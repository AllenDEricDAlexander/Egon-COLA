package top.egon.cola.archetype.source.light.facade.teaching;

import top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO;

public interface CourseFacade {
    CourseDTO createCourse(CreateCourseDTO request);

    CourseDTO getCourse(Long courseId);

}
