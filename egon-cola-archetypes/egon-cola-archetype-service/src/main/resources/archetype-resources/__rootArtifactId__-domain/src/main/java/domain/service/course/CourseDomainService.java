#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.course;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.BizException;
import ${package}.domain.repos.course.CourseRepository;

public class CourseDomainService {

    private final CourseRepository courseRepository;

    public CourseDomainService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public void ensureCourseNameAvailable(String name) {
        if (courseRepository.existsByName(name)) {
            throw new BizException(ErrorCodes.COURSE_NAME_DUPLICATED, "course name already exists");
        }
    }
}
