package top.egon.cola.component.accessguard.reject;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

public interface RejectResponseInvoker {

    Object reject(ProceedingJoinPoint joinPoint, AccessGuardRule rule, AccessGuardContext context, Object[] args);
}
