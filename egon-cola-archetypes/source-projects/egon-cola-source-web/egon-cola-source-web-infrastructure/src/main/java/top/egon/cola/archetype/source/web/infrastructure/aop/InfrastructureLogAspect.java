package top.egon.cola.archetype.source.web.infrastructure.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Logs the outbound client and message-broker failure boundaries once; the DAO timings stay in
 * {@link DaoMonitorAspect} so no operation is timed or logged twice.
 */
@Aspect
@Component("infrastructureLogAspect")
@Slf4j
public class InfrastructureLogAspect {

    // ``within`` keeps the broker branch on the publisher type itself: the in-memory fallback in the
    // local wiring inherits MqMessageService.publish and would otherwise be advised as an mq type.
    @Around("execution(* top.egon.cola.archetype.source.web.infrastructure..client.impl..*.*(..))"
            + " || (execution(* *(..)) && within(top.egon.cola.archetype.source.web.infrastructure.mq.impl..*))")
    public Object logFailure(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (RuntimeException exception) {
            log.error("Infrastructure call failed: {}", joinPoint.getSignature().toShortString(), exception);
            throw exception;
        }
    }
}
