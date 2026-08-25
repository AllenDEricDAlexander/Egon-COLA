#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.service;

import ${package}.domain.course.entities.Course;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.vos.ExamId;
import java.time.Instant;
import java.util.Optional;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

public interface ExamDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    Exam createExam(Course course, String title, Instant startsAt, Instant endsAt);
    ExamPaper attachPaper(Exam exam, String title, int totalPoints);
    Exam publishExam(Exam exam, ExamPaper paper);
    Exam save(Exam exam);
    Optional<Exam> findById(ExamId examId);
    ExamPaper savePaper(ExamPaper paper);
    Optional<ExamPaper> findPaperByExamId(ExamId examId);
}
