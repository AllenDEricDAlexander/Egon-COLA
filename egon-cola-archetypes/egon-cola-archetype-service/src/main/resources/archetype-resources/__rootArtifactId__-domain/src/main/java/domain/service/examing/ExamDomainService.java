#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.examing;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.NotFoundException;
import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.repos.course.CourseRepository;

public class ExamDomainService {

    private final CourseRepository courseRepository;

    public ExamDomainService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public void record(String courseId, String studentId, int score) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
        ExamResult.validate(courseId, studentId, score);
    }
}
