package top.egon.cola.archetype.source.light.domain.teaching.service;

import jakarta.validation.constraints.NotBlank;

/** Request-claim capability owned by the teaching domain. */
public interface CourseIdempotencyService {
    boolean claim(@NotBlank String key);
}
