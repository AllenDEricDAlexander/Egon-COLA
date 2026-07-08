package top.egon.cola.component.accessguard.circuitbreaker;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

public interface TimeoutCircuitBreakerExecutor {

    Object execute(ProceedingJoinPoint joinPoint, AccessGuardRule rule, AccessGuardContext context) throws Throwable;
}
