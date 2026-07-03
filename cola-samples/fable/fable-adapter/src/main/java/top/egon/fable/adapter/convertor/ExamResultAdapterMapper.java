package top.egon.fable.adapter.convertor;

import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.facade.dto.examing.ExamResultDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExamResultAdapterMapper extends BaseMapper<ExamResult, ExamResultDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    ExamResultDTO convert(ExamResult examResult);
}
