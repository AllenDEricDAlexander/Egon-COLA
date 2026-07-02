package top.egon.fable.domain.service.course;

import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.domain.repos.course.CourseRepository;

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
