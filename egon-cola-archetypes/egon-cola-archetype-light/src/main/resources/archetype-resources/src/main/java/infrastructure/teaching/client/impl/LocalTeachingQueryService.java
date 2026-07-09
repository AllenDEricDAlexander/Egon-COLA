package ${package}.infrastructure.teaching.client.impl;

import ${package}.domain.teaching.service.TeachingQueryService;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;

import java.util.Optional;

public final class LocalTeachingQueryService implements TeachingQueryService {
    @Override
    public Optional<ExternalCourse> findExternalCourse(CourseCode code) {
        if (!"COURSE-001".equals(code.value())) {
            return Optional.empty();
        }
        return Optional.of(new ExternalCourse(code, "Local Course"));
    }
}
