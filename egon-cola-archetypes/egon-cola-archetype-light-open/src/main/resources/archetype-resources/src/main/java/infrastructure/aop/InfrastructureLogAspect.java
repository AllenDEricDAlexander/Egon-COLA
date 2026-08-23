package ${package}.infrastructure.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class InfrastructureLogAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfrastructureLogAspect.class);

    @Around("execution(* ${package}.infrastructure..*(..))"
            + " && (within(*..*Client) || within(*..client..*)"
            + " || within(*..cache..*) || within(*..mq..*))")
    public Object logFailure(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (RuntimeException exception) {
            LOGGER.error("Infrastructure call failed: {}", joinPoint.getSignature().toShortString(), exception);
            throw exception;
        }
    }
}
