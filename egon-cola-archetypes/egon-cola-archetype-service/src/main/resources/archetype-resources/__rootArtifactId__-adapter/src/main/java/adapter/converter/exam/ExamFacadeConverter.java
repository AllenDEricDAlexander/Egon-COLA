#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.converter.exam;
import ${package}.application.command.exam.AttachExamPaperCommand;
import ${package}.application.command.exam.CreateExamCommand;
import ${package}.application.command.exam.PublishExamCommand;
import ${package}.application.result.exam.ExamDetailResult;
import ${package}.application.result.exam.ExamPaperResult;
import ${package}.facade.dto.exam.AttachExamPaperRequest;
import ${package}.facade.dto.exam.CreateExamRequest;
import ${package}.facade.dto.exam.ExamPaperResponse;
import ${package}.facade.dto.exam.ExamResponse;
import ${package}.facade.dto.exam.PublishExamRequest;
import org.springframework.stereotype.Component;
@Component
public class ExamFacadeConverter {
    public CreateExamCommand toCommand(CreateExamRequest request) {
        return new CreateExamCommand(request.courseId(), request.title(), request.startsAt(), request.endsAt());
    }
    public AttachExamPaperCommand toCommand(AttachExamPaperRequest request) {
        return new AttachExamPaperCommand(request.examId(), request.title(), request.totalPoints());
    }
    public PublishExamCommand toCommand(PublishExamRequest request) { return new PublishExamCommand(request.examId()); }
    public ExamResponse toResponse(ExamDetailResult result) {
        return new ExamResponse(result.id(), result.courseId(), result.title(), result.startsAt(), result.endsAt(), result.status());
    }
    public ExamPaperResponse toResponse(ExamPaperResult result) {
        return new ExamPaperResponse(result.id(), result.examId(), result.title(), result.totalPoints(), result.status());
    }
}
