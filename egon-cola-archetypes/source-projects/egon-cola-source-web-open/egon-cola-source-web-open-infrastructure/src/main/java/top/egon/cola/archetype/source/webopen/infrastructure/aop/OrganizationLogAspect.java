package top.egon.cola.archetype.source.webopen.infrastructure.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Logs the outbound client and message-broker failure boundary exactly once. */
@Aspect
@Component("organizationLogAspect")
@Slf4j
public class OrganizationLogAspect {

    // ``within`` keeps the broker branch on the publisher type itself: the local fallback in
    // OrganizationLocalFallbackConfig inherits MqMessageService.publish and would otherwise be
    // advised as an mq type.
    @Around("execution(* top.egon.cola.archetype.source.webopen.infrastructure..client.impl..*.*(..))"
            + " || (execution(* *(..)) && within(top.egon.cola.archetype.source.webopen.infrastructure.mq.impl..*))")
    public Object log(ProceedingJoinPoint point) throws Throwable {
        try {
            return point.proceed();
        } catch (RuntimeException failure) {
            log.error("Infrastructure call failed: {}",
                    point.getSignature() == null ? "unknown" : point.getSignature().toShortString(), failure);
            throw failure;
        }
    }
}
