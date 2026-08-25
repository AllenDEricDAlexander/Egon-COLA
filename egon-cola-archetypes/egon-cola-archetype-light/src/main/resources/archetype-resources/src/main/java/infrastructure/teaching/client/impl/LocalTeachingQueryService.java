package ${package}.infrastructure.teaching.client.impl;

import ${package}.domain.teaching.gateway.TeachingQueryGateway;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class LocalTeachingQueryService implements TeachingQueryGateway {
    @Override
    public Optional<ExternalCourse> findExternalCourse(CourseCode code) {
        if (!"COURSE-001".equals(code.value())) {
            return Optional.empty();
        }
        return Optional.of(new ExternalCourse(code, "Local Course"));
    }
}
