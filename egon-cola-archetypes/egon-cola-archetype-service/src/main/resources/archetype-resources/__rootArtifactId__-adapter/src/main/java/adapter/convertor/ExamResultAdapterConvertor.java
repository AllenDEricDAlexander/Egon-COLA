#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.enums.ExamResultStatus;
import ${package}.facade.dto.examing.ExamResultDTO;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("examResultAdapterConvertor")
@RequiredArgsConstructor
public class ExamResultAdapterConvertor {

    @Qualifier("converter")
    private final Converter converter;

    public ExamResultDTO toDTO(ExamResult examResult) {
        ExamResultDTO examResultDTO = converter.convert(examResult, ExamResultDTO.class);
        examResultDTO.setStatus(toFacadeStatus(examResult.getStatus(), examResult.getScore()));
        return examResultDTO;
    }

    private String toFacadeStatus(ExamResultStatus status, int score) {
        if (ExamResultStatus.RECORDED == status) {
            return score >= 60 ? "PASSED" : "FAILED";
        }
        return status.name();
    }

    @Mapper(componentModel = "spring")
    public interface ExamResultMapper extends BaseMapper<ExamResult, ExamResultDTO> {

        @Override
        @Mapping(target = "status", ignore = true)
        ExamResultDTO convert(ExamResult examResult);
    }
}
