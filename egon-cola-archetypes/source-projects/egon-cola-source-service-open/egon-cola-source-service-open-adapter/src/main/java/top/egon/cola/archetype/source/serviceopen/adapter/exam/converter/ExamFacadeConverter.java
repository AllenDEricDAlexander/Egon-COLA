package top.egon.cola.archetype.source.serviceopen.adapter.exam.converter;

import top.egon.cola.archetype.source.serviceopen.application.exam.command.AttachExamPaperCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.command.CreateExamCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.command.PublishExamCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ExamDetailResult;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ExamPaperResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Exam;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.AttachExamPaperRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PublishExamRequest;
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
