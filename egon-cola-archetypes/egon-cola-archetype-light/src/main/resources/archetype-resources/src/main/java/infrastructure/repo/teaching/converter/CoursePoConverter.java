package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;

import java.time.LocalDateTime;

public final class CoursePoConverter {
    private CoursePoConverter() {
    }

    public static CoursePo toPo(Course course) {
        return new CoursePo(course.getId(), course.getName(), course.getDescription(), LocalDateTime.now());
    }

    public static Course toDomain(CoursePo coursePo) {
        return Course.create(coursePo.getId(), coursePo.getName(), coursePo.getDescription());
    }
}
