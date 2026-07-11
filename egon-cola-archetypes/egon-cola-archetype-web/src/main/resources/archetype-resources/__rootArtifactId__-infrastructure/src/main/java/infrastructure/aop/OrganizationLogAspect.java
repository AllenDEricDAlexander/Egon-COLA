package ${package}.infrastructure.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component("organizationLogAspect")
public class OrganizationLogAspect {
    private static final Logger log = LoggerFactory.getLogger(OrganizationLogAspect.class);

    @Around("execution(public * ${package}.application.manage..*(..))")
    public Object log(ProceedingJoinPoint point) throws Throwable {
        long started = System.nanoTime();
        try {
            return point.proceed();
        } finally {
            long elapsedNanos = System.nanoTime() - started;
            String method = point.getSignature() == null ? "unknown" : point.getSignature().toShortString();
            log.info("traceId={} method={} elapsedNanos={}", traceId(), method, elapsedNanos);
        }
    }

    private static String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null ? "unknown" : traceId;
    }
}
