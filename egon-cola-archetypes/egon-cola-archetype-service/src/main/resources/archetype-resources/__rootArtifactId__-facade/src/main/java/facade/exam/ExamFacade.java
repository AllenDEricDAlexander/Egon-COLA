#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.exam;

import ${package}.facade.dto.SingleResponse;
import ${package}.facade.exam.dto.AttachExamPaperRequest;
import ${package}.facade.exam.dto.CreateExamRequest;
import ${package}.facade.exam.dto.ExamPaperResponse;
import ${package}.facade.exam.dto.ExamResponse;
import ${package}.facade.exam.dto.GetExamRequest;
import ${package}.facade.exam.dto.PublishExamRequest;

public interface ExamFacade {

    SingleResponse<ExamResponse> createExam(CreateExamRequest request);

    SingleResponse<ExamPaperResponse> attachPaper(AttachExamPaperRequest request);

    SingleResponse<ExamResponse> publishExam(PublishExamRequest request);

    SingleResponse<ExamResponse> getExam(GetExamRequest request);
}
