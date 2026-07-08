package top.egon.cola.component.accessguard.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AccessGuardAop {

    @Around("@annotation(top.egon.cola.component.accessguard.annotation.AccessGuard)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.WhiteListAccessInterceptor)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.RateLimiterAccessInterceptor)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.TimeoutCircuitBreaker)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoWhiteList)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoRateLimiter)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoHystrix)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
