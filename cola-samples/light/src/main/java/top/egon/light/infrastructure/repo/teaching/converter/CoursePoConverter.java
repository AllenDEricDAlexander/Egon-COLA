package top.egon.light.infrastructure.repo.teaching.converter;

import top.egon.light.domain.teaching.model.Course;
import top.egon.light.infrastructure.repo.teaching.po.CoursePo;

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
