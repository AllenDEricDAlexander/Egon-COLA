#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.evaluation;

import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import ${package}.domain.client.evaluation.EvaluationCourse;
import ${package}.domain.client.evaluation.EvaluationExam;
import ${package}.domain.client.evaluation.EvaluationQueryPort;
import ${package}.domain.client.evaluation.EvaluationScore;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class LocalEvaluationQueryStub implements EvaluationQueryPort {

    @Override
    public EvaluationCourse getCourse(String courseId) {
        rejectMissing(courseId, "course");
        return new EvaluationCourse(courseId, "LOCAL", "Local Course " + courseId, 0, "ACTIVE");
    }

    @Override
    public EvaluationExam getExam(String examId) {
        rejectMissing(examId, "exam");
        return new EvaluationExam(
                examId,
                "local-course",
                "Local Exam " + examId,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                "PUBLISHED");
    }

    @Override
    public EvaluationScore getScore(String scoreId) {
        rejectMissing(scoreId, "score");
        return new EvaluationScore(
                scoreId, "local-exam", "local-course", "local-student", 100, "RECORDED");
    }

    private static void rejectMissing(String id, String resource) {
        if (id == null || id.isBlank() || id.startsWith("missing-")) {
            throw new ExternalDependencyException(
                    "evaluation",
                    ExternalDependencyFailure.NOT_FOUND,
                    "LOCAL_NOT_FOUND",
                    "local evaluation " + resource + " was not found",
                    null);
        }
    }
}
