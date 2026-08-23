package ${package}.infrastructure;

import ${package}.infrastructure.aop.OrganizationLogAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
