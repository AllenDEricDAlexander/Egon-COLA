package top.egon.cola.component.common.mybatis.routing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Canonical addresses plus the fingerprint of the complete policy used to resolve them.
 */
public record EgonColaRouteResult(
        @NotEmpty List<@NotNull @Valid EgonColaPhysicalTargetBO> targets,
        @NotNull @Pattern(regexp = "[0-9a-f]{64}") String fingerprint) {

    public EgonColaRouteResult {
        if (targets == null || targets.isEmpty() || fingerprint == null || !fingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Route result requires targets and a SHA-256 fingerprint");
        }
        targets = targets.stream().distinct().sorted(EgonColaPhysicalTargetBO.ORDER).toList();
    }
}
