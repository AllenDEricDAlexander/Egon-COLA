package top.egon.cola.archetype.source.lightopen.facade.teaching;

import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CourseDTO;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateCourseDTO;

public interface CourseFacade {
    CourseDTO createCourse(CreateCourseDTO request);

    CourseDTO getCourse(Long courseId);

}
