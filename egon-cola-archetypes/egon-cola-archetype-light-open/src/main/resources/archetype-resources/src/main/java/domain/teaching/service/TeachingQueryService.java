package ${package}.domain.teaching.service;

import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;

import java.util.Optional;

public interface TeachingQueryService {
    Optional<ExternalCourse> findExternalCourse(CourseCode code);
}
