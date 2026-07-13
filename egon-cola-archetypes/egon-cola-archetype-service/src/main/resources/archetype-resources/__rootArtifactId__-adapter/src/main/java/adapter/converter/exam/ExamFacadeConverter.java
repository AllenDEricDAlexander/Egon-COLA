#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.converter.exam;
import ${package}.application.exam.command.AttachExamPaperCommand;
import ${package}.application.exam.command.CreateExamCommand;
import ${package}.application.exam.command.PublishExamCommand;
import ${package}.application.exam.result.ExamDetailResult;
import ${package}.application.exam.result.ExamPaperResult;
import ${package}.facade.exam.dto.AttachExamPaperRequest;
import ${package}.facade.exam.dto.CreateExamRequest;
import ${package}.facade.exam.dto.ExamPaperResponse;
import ${package}.facade.exam.dto.ExamResponse;
import ${package}.facade.exam.dto.PublishExamRequest;
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
