#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.converter;

import ${package}.application.exam.command.AttachExamPaperCommand;
import ${package}.application.exam.command.CreateExamCommand;
import ${package}.application.exam.command.PublishExamCommand;
import ${package}.application.exam.result.ExamDetailResult;
import ${package}.application.exam.result.ExamPaperResult;
import ${package}.facade.evaluation.v1.Exam;
import ${package}.facade.evaluation.v1.ExamPaper;
import ${package}.facade.evaluation.v1.AttachExamPaperRequest;
import ${package}.facade.evaluation.v1.CreateExamRequest;
import ${package}.facade.evaluation.v1.PublishExamRequest;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ExamFacadeConverter {

    public CreateExamCommand toCommand(CreateExamRequest request) {
        Objects.requireNonNull(request, "request");
        return new CreateExamCommand(request.getCourseId(), request.getTitle(),
                toInstant(request.getStartsAt()), toInstant(request.getEndsAt()));
    }

    public AttachExamPaperCommand toCommand(AttachExamPaperRequest request) {
        Objects.requireNonNull(request, "request");
        return new AttachExamPaperCommand(request.getExamId(), request.getTitle(), request.getTotalPoints());
    }

    public PublishExamCommand toCommand(PublishExamRequest request) {
        Objects.requireNonNull(request, "request");
        return new PublishExamCommand(request.getExamId());
    }

    public Exam toResponse(ExamDetailResult result) {
        Objects.requireNonNull(result, "result");
        return Exam.newBuilder()
                .setId(result.id())
                .setCourseId(result.courseId())
                .setTitle(result.title())
                .setStartsAt(toTimestamp(result.startsAt()))
                .setEndsAt(toTimestamp(result.endsAt()))
                .setStatus(result.status())
                .build();
    }

    public ExamPaper toResponse(ExamPaperResult result) {
        Objects.requireNonNull(result, "result");
        return ExamPaper.newBuilder()
                .setId(result.id())
                .setExamId(result.examId())
                .setTitle(result.title())
                .setTotalPoints(result.totalPoints())
                .setStatus(result.status())
                .build();
    }

    private static Instant toInstant(Timestamp timestamp) {
        Objects.requireNonNull(timestamp, "timestamp");
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private static Timestamp toTimestamp(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
