package ${package}.domain.teaching.gateway;

import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;

import java.util.Optional;

/** Outbound query gateway owned by the teaching domain. */
public interface TeachingQueryGateway {
    Optional<ExternalCourse> findExternalCourse(CourseCode code);
}
