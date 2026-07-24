#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.converter;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.exam.vos.ScoreValue;
import ${package}.infrastructure.exam.repo.po.ScorePo;
import java.time.Instant;
import org.springframework.stereotype.Component;
@Component
public class ScoreConverter {
    public ScorePo toPo(Score score, Instant createdAt) {
        return new ScorePo(score.getId(), score.getExamId().value(), score.getCourseId().value(),
                score.getStudentId(), score.getPoints().value(), score.getStatus().name(), createdAt, Instant.now());
    }
    public ScorePo updatePo(Score score, ScorePo po) {
        po.update(score.getCourseId().value(), score.getStudentId(), score.getPoints().value(),
                score.getStatus().name(), Instant.now());
        return po;
    }
    public Score toDomain(ScorePo po) {
        return new Score(po.getId(), new ExamId(po.getExamId()), new CourseId(po.getCourseId()),
                po.getStudentId(), new ScoreValue(po.getPoints()), ScoreStatus.valueOf(po.getStatus()));
    }
}
