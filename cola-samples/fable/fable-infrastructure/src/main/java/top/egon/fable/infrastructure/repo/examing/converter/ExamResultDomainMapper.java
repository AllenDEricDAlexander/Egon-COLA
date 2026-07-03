package top.egon.fable.infrastructure.repo.examing.converter;

import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.infrastructure.repo.examing.po.ExamResultPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExamResultDomainMapper extends BaseMapper<ExamResultPo, ExamResult> {

    @Override
    @Mapping(target = "status", ignore = true)
    ExamResult convert(ExamResultPo examResultPo);

    @Override
    @Mapping(target = "status", ignore = true)
    ExamResult convert(ExamResultPo examResultPo, @MappingTarget ExamResult examResult);
}
