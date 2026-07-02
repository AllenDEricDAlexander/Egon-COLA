#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.api;

import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.examing.ExamResultDTO;
import ${package}.facade.dto.examing.RecordExamResultRequest;

public interface ExamResultFacade {

    SingleResponse<ExamResultDTO> record(RecordExamResultRequest request);

    SingleResponse<ExamResultDTO> getResult(String examResultId);
}
