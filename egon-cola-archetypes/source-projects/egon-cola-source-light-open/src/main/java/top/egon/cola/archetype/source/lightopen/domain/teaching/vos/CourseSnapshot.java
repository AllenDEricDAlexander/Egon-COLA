package top.egon.cola.archetype.source.lightopen.domain.teaching.vos;

import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.CourseStatus;

public record CourseSnapshot(Long id, CourseCode code, String name, CourseStatus status) {
    public static CourseSnapshot from(Course course) {
        return new CourseSnapshot(course.id(), course.code(), course.name(), course.status());
    }

}
