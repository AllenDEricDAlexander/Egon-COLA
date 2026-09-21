package top.egon.cola.archetype.source.lightopen.domain.teaching.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.ExternalCourse;

import java.util.Optional;

/** Outbound teaching query capability owned by the domain. */
public interface TeachingQueryService {
    Optional<ExternalCourse> findExternalCourse(@Valid @NotNull CourseCode code);
}
