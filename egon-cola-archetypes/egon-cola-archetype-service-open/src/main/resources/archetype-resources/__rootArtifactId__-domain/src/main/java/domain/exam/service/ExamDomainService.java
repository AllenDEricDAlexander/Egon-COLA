#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.service;

import ${package}.domain.course.entities.Course;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import java.time.Instant;

public interface ExamDomainService {
    Exam createExam(String id, Course course, String title, Instant startsAt, Instant endsAt);
    ExamPaper attachPaper(String id, Exam exam, String title, int totalPoints);
    Exam publishExam(Exam exam, ExamPaper paper);
}
