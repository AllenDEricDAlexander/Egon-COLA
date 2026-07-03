package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

@Component("courseDomainFactory")
public class CourseDomainFactory {
    @ObjectFactory
    public Course create(CoursePo coursePo) {
        return Course.create(coursePo.getId(), coursePo.getName(), coursePo.getDescription());
    }
}
