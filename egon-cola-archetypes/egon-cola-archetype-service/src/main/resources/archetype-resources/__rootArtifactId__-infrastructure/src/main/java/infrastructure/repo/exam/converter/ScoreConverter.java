#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam.converter;
import ${package}.domain.entities.exam.Score;
import ${package}.domain.enums.exam.ScoreStatus;
import ${package}.domain.vos.course.CourseId;
import ${package}.domain.vos.exam.ExamId;
import ${package}.domain.vos.exam.ScoreValue;
import ${package}.infrastructure.repo.exam.po.ScorePo;
import java.time.Instant;
import org.springframework.stereotype.Component;
@Component
public class ScoreConverter {
    public ScorePo toPo(Score score, Instant createdAt) {
        return new ScorePo(score.getId(), score.getExamId().value(), score.getCourseId().value(),
                score.getStudentId(), score.getPoints().value(), score.getStatus().name(), createdAt, Instant.now());
    }
    public Score toDomain(ScorePo po) {
        return new Score(po.getId(), new ExamId(po.getExamId()), new CourseId(po.getCourseId()),
                po.getStudentId(), new ScoreValue(po.getPoints()), ScoreStatus.valueOf(po.getStatus()));
    }
}
