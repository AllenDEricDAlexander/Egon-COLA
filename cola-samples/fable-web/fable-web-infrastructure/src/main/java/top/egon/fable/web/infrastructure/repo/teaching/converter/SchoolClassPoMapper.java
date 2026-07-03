package top.egon.fable.web.infrastructure.repo.teaching.converter;

import top.egon.fable.web.domain.entities.teaching.SchoolClass;
import top.egon.fable.web.infrastructure.repo.teaching.po.SchoolClassPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SchoolClassPoMapper extends BaseMapper<SchoolClass, SchoolClassPo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    SchoolClassPo convert(SchoolClass schoolClass);
}
