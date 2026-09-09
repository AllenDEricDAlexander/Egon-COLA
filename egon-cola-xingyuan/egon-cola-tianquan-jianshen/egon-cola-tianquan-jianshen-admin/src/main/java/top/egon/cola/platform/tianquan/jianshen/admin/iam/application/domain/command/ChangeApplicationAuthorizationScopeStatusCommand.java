package top.egon.cola.platform.tianquan.jianshen.admin.iam.application.domain.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Optimistic-concurrency request to change a local Application scope status. */
public record ChangeApplicationAuthorizationScopeStatusCommand(
        @NotBlank String status,
        @PositiveOrZero long expectedVersion) {
}
