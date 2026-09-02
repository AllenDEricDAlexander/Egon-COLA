package top.egon.cola.archetype.source.lightopen.domain.teaching.gateway;

import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.ExternalCourse;

import java.util.Optional;

/** Outbound query gateway owned by the teaching domain. */
public interface TeachingQueryGateway {
    Optional<ExternalCourse> findExternalCourse(CourseCode code);
}
