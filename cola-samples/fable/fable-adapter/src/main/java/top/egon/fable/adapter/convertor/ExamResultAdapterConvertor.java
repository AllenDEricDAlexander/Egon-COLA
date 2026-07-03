package top.egon.fable.adapter.convertor;

import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.domain.enums.ExamResultStatus;
import top.egon.fable.facade.dto.examing.ExamResultDTO;
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
