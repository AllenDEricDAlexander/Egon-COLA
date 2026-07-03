package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("schoolClassDomainFactory")
public class SchoolClassDomainFactory {
    @ObjectFactory
    public SchoolClass create(SchoolClassPo schoolClassPo) {
        return SchoolClass.restore(schoolClassPo.getId(), schoolClassPo.getName(), schoolClassPo.getGradeName(), List.of());
    }
}
