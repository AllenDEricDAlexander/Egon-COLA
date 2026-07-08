package top.egon.cola.component.accessguard.key;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;

public interface AccessKeyResolver {

    AccessKeyResolution resolve(ProceedingJoinPoint joinPoint, AccessGuardRule rule);
}
