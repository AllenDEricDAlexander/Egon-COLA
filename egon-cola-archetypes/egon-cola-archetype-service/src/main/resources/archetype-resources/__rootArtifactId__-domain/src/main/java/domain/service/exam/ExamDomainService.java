#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.exam;

import ${package}.domain.entities.course.Course;
import ${package}.domain.entities.exam.Exam;
import ${package}.domain.entities.exam.ExamPaper;
import java.time.Instant;

public interface ExamDomainService {
    Exam createExam(String id, Course course, String title, Instant startsAt, Instant endsAt);
    ExamPaper attachPaper(String id, Exam exam, String title, int totalPoints);
    Exam publishExam(Exam exam, ExamPaper paper);
}
