#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.api;

import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.exam.AttachExamPaperRequest;
import ${package}.facade.dto.exam.CreateExamRequest;
import ${package}.facade.dto.exam.ExamPaperResponse;
import ${package}.facade.dto.exam.ExamResponse;
import ${package}.facade.dto.exam.GetExamRequest;
import ${package}.facade.dto.exam.PublishExamRequest;

public interface ExamFacade {

    SingleResponse<ExamResponse> createExam(CreateExamRequest request);

    SingleResponse<ExamPaperResponse> attachPaper(AttachExamPaperRequest request);

    SingleResponse<ExamResponse> publishExam(PublishExamRequest request);

    SingleResponse<ExamResponse> getExam(GetExamRequest request);
}
