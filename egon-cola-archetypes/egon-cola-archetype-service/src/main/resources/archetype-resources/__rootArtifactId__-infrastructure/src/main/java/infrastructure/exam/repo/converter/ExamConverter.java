#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.converter;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.po.ExamPo;
import java.time.Instant;
import org.springframework.stereotype.Component;
@Component
public class ExamConverter {
    public ExamPo toPo(Exam exam) { return toPo(exam, Instant.now()); }
    public ExamPo toPo(Exam exam, Instant createdAt) {
        return new ExamPo(exam.getId().value(), exam.getCourseId().value(), exam.getTitle(),
                exam.getStartsAt(), exam.getEndsAt(), exam.getStatus().name(), createdAt, Instant.now());
    }
    public Exam toDomain(ExamPo po) {
        return new Exam(new ExamId(po.getId()), new CourseId(po.getCourseId()), po.getTitle(),
                po.getStartsAt(), po.getEndsAt(), ExamStatus.valueOf(po.getStatus()));
    }
}
