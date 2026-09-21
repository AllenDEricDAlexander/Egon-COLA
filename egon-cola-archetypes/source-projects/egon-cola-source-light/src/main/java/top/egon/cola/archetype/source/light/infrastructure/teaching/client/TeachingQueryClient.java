package top.egon.cola.archetype.source.light.infrastructure.teaching.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.domain.teaching.vos.ExternalCourse;

import java.util.Optional;

/** Transport-facing teaching query client; profiles bind a local or HTTP implementation. */
public interface TeachingQueryClient {
    Optional<ExternalCourse> findExternalCourse(@Valid @NotNull CourseCode code);
}
