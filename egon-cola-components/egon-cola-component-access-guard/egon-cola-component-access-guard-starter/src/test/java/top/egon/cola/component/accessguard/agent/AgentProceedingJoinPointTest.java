package top.egon.cola.component.accessguard.agent;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentProceedingJoinPointTest {

    @Test
    void invokesInstanceContinuationOnceWithDefensiveArguments() throws Throwable {
        Target target = new Target();
        Method method = Target.class.getDeclaredMethod("instance", String.class);
        Object[] arguments = {"first"};
        var handle = MethodHandles.lookup().findVirtual(
                Target.class, "body", MethodType.methodType(String.class, String.class));
        AgentProceedingJoinPoint joinPoint = new AgentProceedingJoinPoint(
                target, method, handle, arguments);

        arguments[0] = "changed";
        Object[] returned = joinPoint.getArgs();
        returned[0] = "changed-again";

        assertThat(joinPoint.proceed()).isEqualTo("body:first");
        assertThat(joinPoint.proceed(new Object[]{"second"})).isEqualTo("body:second");
        assertThat(target.calls).isEqualTo(2);
        assertThat(joinPoint.getThis()).isSameAs(target);
        assertThat(joinPoint.getKind()).isEqualTo(JoinPoint.METHOD_EXECUTION);
        assertThat(((MethodSignature) joinPoint.getSignature()).getMethod()).isSameAs(method);
    }

    @Test
    void invokesStaticContinuationAndPreservesThrowableIdentity() throws Throwable {
        Method method = Target.class.getDeclaredMethod("statik", String.class);
        var handle = MethodHandles.lookup().findStatic(
                Target.class, "staticBody", MethodType.methodType(String.class, String.class));
        AgentProceedingJoinPoint joinPoint = new AgentProceedingJoinPoint(
                null, method, handle, new Object[]{"ok"});

        assertThat(joinPoint.proceed()).isEqualTo("static:ok");
        assertThat(joinPoint.getTarget()).isNull();

        RuntimeException failure = Target.FAILURE;
        assertThatThrownBy(() -> joinPoint.proceed(new Object[]{"fail"})).isSameAs(failure);
    }

    static class Target {

        static final RuntimeException FAILURE = new RuntimeException("sentinel");
        int calls;

        String instance(String value) {
            return value;
        }

        String body(String value) {
            calls++;
            return "body:" + value;
        }

        static String statik(String value) {
            return value;
        }

        static String staticBody(String value) {
            if (value.equals("fail")) {
                throw FAILURE;
            }
            return "static:" + value;
        }
    }
}
