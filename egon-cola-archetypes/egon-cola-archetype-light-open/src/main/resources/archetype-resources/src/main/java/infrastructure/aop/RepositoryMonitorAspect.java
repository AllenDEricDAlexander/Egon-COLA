package ${package}.infrastructure.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RepositoryMonitorAspect {
    private final MeterRegistry meterRegistry;

    @Around("execution(* ${package}.infrastructure..*(..))"
            + " && (within(*..*Repository) || within(*..repo..*))")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(Timer.builder("infrastructure.repository")
                    .tag("method", joinPoint.getSignature().toShortString())
                    .register(meterRegistry));
        }
    }
}
