package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SchoolClassDomainFactory.class)
public interface SchoolClassDomainMapper extends BaseMapper<SchoolClassPo, SchoolClass> {
    @Override
    @Mapping(target = "userIds", ignore = true)
    SchoolClass convert(SchoolClassPo schoolClassPo);
}
