package top.egon.cola.archetype.source.web.domain.service;

import jakarta.validation.constraints.NotBlank;

/** Request-claim capability owned by the organization domain. */
public interface CommandIdempotencyService {

    boolean claim(@NotBlank String operation, @NotBlank String requestId);

    void release(@NotBlank String operation, @NotBlank String requestId);
}
