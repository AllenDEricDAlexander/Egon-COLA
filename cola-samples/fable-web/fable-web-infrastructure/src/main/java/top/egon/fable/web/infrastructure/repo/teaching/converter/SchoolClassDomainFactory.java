package top.egon.fable.web.infrastructure.repo.teaching.converter;

import top.egon.fable.web.domain.entities.teaching.SchoolClass;
import top.egon.fable.web.infrastructure.repo.teaching.po.SchoolClassPo;
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
