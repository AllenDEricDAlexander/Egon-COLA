package top.egon.cola.archetype.source.lightopen.domain.user.service;

import jakarta.validation.constraints.NotBlank;

/** Request-claim capability owned by the user domain. */
public interface UserIdempotencyService {
    boolean claim(@NotBlank String key);
}
