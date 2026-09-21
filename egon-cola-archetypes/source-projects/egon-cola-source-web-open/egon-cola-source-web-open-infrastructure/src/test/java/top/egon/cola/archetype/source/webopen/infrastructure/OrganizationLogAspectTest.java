package top.egon.cola.archetype.source.webopen.infrastructure;

import top.egon.cola.archetype.source.webopen.infrastructure.aop.OrganizationLogAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationLogAspectTest {
    @Mock ProceedingJoinPoint joinPoint;

    @Test
    void rethrowsOriginalFailure() throws Throwable {
        IllegalStateException failure = new IllegalStateException("boom");
        when(joinPoint.proceed()).thenThrow(failure);

        Throwable thrown = assertThrows(Throwable.class,
            () -> new OrganizationLogAspect().log(joinPoint));

        assertSame(failure, thrown);
    }

    @Test
    void advisesTheClientAndBrokerFailureBoundariesOnly() throws NoSuchMethodException {
        String pointcut = OrganizationLogAspect.class
                .getMethod("log", ProceedingJoinPoint.class)
                .getAnnotation(Around.class)
                .value();

        assertTrue(pointcut.contains("infrastructure..client.impl..*.*(..)"),
                "the moved Client implementations must stay inside the log range: " + pointcut);
        assertTrue(pointcut.contains("within(top.egon.cola.archetype.source.webopen.infrastructure.mq.impl..*)"),
                "only the generic MQ implementation, not its local fallback, may be advised: " + pointcut);
        for (String retired : new String[] {"cache", "repo", "converter", "application.manage"}) {
            assertTrue(!pointcut.contains(retired),
                    "the log range must drop the retired " + retired + " scope: " + pointcut);
        }
    }
}
