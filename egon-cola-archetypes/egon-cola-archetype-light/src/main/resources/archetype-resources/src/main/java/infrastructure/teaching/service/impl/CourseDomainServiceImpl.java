package ${package}.infrastructure.teaching.service.impl;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.service.CourseDomainService;
import ${package}.domain.teaching.vos.CourseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.IdGenerator;

@Service("courseDomainService")
@RequiredArgsConstructor
public class CourseDomainServiceImpl implements CourseDomainService {
    private final IdGenerator idGenerator;

    @Override
    public Course createCourse(CourseCode code, String name) {
        return new Course(idGenerator.nextId(), code, name, CourseStatus.ACTIVE);
    }
}
