#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.converter;

import ${package}.application.exam.command.AttachExamPaperCommand;
import ${package}.application.exam.command.CreateExamCommand;
import ${package}.application.exam.command.PublishExamCommand;
import ${package}.application.exam.result.ExamDetailResult;
import ${package}.application.exam.result.ExamPaperResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.evaluation.facade.exam.dto.AttachExamPaperRequest;
import top.egon.cola.evaluation.facade.exam.dto.CreateExamRequest;
import top.egon.cola.evaluation.facade.exam.dto.ExamPaperResponse;
import top.egon.cola.evaluation.facade.exam.dto.ExamResponse;
import top.egon.cola.evaluation.facade.exam.dto.PublishExamRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ExamFacadeConverter {

    CreateExamCommand toCommand(CreateExamRequest request);

    AttachExamPaperCommand toCommand(AttachExamPaperRequest request);

    PublishExamCommand toCommand(PublishExamRequest request);

    ExamResponse toResponse(ExamDetailResult result);

    ExamPaperResponse toResponse(ExamPaperResult result);
}
