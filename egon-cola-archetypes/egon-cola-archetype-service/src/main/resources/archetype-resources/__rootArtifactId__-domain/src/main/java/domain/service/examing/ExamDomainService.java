#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.examing;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.NotFoundException;
import ${package}.domain.client.course.CourseClient;
import ${package}.domain.entities.examing.ExamResult;

public class ExamDomainService {

    private final CourseClient courseClient;

    public ExamDomainService(CourseClient courseClient) {
        this.courseClient = courseClient;
    }

    public void record(String courseId, String studentId, int score) {
        courseClient.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
        ExamResult.validate(courseId, studentId, score);
    }
}
