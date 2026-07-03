package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SchoolClassPoMapper extends BaseMapper<SchoolClass, SchoolClassPo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    SchoolClassPo convert(SchoolClass schoolClass);
}
