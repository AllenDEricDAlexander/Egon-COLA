package top.egon.light.infrastructure.repo.teaching.converter;

import top.egon.light.domain.teaching.model.Course;
import top.egon.light.infrastructure.repo.teaching.po.CoursePo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

@Component("courseDomainFactory")
public class CourseDomainFactory {
    @ObjectFactory
    public Course create(CoursePo coursePo) {
        return Course.create(coursePo.getId(), coursePo.getName(), coursePo.getDescription());
    }
}
