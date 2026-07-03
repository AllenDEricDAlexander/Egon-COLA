#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.enums.ExamResultStatus;
import ${package}.facade.dto.examing.ExamResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("examResultAdapterConvertor")
@RequiredArgsConstructor
public class ExamResultAdapterConvertor {

    @Qualifier("examResultAdapterMapperImpl")
    private final ExamResultAdapterMapper examResultAdapterMapper;

    public ExamResultDTO toDTO(ExamResult examResult) {
        ExamResultDTO examResultDTO = examResultAdapterMapper.convert(examResult);
        examResultDTO.setStatus(toFacadeStatus(examResult.getStatus(), examResult.getScore()));
        return examResultDTO;
    }

    private String toFacadeStatus(ExamResultStatus status, int score) {
        if (ExamResultStatus.RECORDED == status) {
            return score >= 60 ? "PASSED" : "FAILED";
        }
        return status.name();
    }
}
