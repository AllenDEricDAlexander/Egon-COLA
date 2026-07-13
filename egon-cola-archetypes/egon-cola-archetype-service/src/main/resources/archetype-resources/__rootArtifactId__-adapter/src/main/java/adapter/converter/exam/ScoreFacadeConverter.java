#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.converter.exam;
import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.result.ScoreResult;
import ${package}.facade.exam.dto.RecordScoreRequest;
import ${package}.facade.exam.dto.ScoreResponse;
import org.springframework.stereotype.Component;
@Component
public class ScoreFacadeConverter {
    public RecordScoreCommand toCommand(RecordScoreRequest request) {
        return new RecordScoreCommand(request.examId(), request.studentId(), request.points());
    }
    public ScoreResponse toResponse(ScoreResult result) {
        return new ScoreResponse(result.id(), result.examId(), result.courseId(),
                result.studentId(), result.points(), result.status());
    }
}
