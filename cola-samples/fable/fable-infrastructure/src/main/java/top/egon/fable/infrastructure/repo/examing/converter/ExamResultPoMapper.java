package top.egon.fable.infrastructure.repo.examing.converter;

import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.infrastructure.repo.examing.po.ExamResultPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExamResultPoMapper extends BaseMapper<ExamResult, ExamResultPo> {

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    ExamResultPo convert(ExamResult examResult);

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    ExamResultPo convert(ExamResult examResult, @MappingTarget ExamResultPo examResultPo);
}
