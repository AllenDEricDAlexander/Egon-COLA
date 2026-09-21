package top.egon.cola.archetype.source.web.infrastructure.aop;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Times the inherited MyBatis operations exposed by the {@code infrastructure.dao} contracts
 * (every {@code *DAO} extends {@code EgonColaMapper}), so persistence latency is measured once per
 * mapper call instead of per repository wrapper invocation. Labels carry only the method signature,
 * never tenant, identifier, SQL or payload content.
 */
@Aspect
@Component("daoMonitorAspect")
@Slf4j
public class DaoMonitorAspect {

    @Around("execution(public * top.egon.cola.archetype.source.web.infrastructure..*DAO.*(..))")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(Metrics.globalRegistry);
        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(Timer.builder("infrastructure.dao")
                    .tag("method", joinPoint.getSignature().toShortString())
                    .register(Metrics.globalRegistry));
        }
    }
}
