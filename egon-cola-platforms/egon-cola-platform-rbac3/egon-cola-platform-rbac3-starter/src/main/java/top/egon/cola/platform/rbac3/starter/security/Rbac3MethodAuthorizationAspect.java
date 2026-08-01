package top.egon.cola.platform.rbac3.starter.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;

import java.util.Objects;

/**
 * Thin method-level PEP delegating all decisions to AuthorizationService.
 */
@Aspect
public final class Rbac3MethodAuthorizationAspect {

    private final AuthorizationService authorizationService;

    public Rbac3MethodAuthorizationAspect(AuthorizationService authorizationService) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService");
    }

    @Around("@annotation(requiresPermission)")
    public Object authorize(
            ProceedingJoinPoint joinPoint,
            RequiresPermission requiresPermission
    ) throws Throwable {
        var decision = authorizationService.requirePermission(
                PermissionRequest.of(requiresPermission.value()));
        if (decision.decision() != Decision.ALLOW) {
            throw new AuthorizationDeniedException(
                    decision.reasonCode(), requiresPermission.value());
        }
        return joinPoint.proceed();
    }
}
