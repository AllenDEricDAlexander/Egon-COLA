package top.egon.fable.web.infrastructure.repo.teaching.converter;

import top.egon.fable.web.domain.entities.teaching.SchoolClass;
import top.egon.fable.web.infrastructure.repo.teaching.po.SchoolClassPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SchoolClassDomainFactory.class)
public interface SchoolClassDomainMapper extends BaseMapper<SchoolClassPo, SchoolClass> {
    @Override
    @Mapping(target = "userIds", ignore = true)
    SchoolClass convert(SchoolClassPo schoolClassPo);
}
