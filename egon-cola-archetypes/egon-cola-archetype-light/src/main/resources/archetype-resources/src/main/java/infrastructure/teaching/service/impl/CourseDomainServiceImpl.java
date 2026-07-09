package ${package}.infrastructure.teaching.service.impl;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.service.CourseDomainService;
import ${package}.domain.teaching.vos.CourseCode;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("courseDomainService")
public class CourseDomainServiceImpl implements CourseDomainService {
    @Override
    public Course createCourse(CourseCode code, String name) {
        return new Course(nextId(), code, name, CourseStatus.ACTIVE);
    }

    private String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
