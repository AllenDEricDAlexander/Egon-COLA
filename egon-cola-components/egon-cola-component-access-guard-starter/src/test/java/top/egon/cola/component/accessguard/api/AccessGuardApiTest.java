package top.egon.cola.component.accessguard.api;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardOutcome;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessGuardApiTest {

    @Test
    void accessGuardBindsOnlyRuleAndOptionalKey() throws Exception {
        assertThat(AccessGuard.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactlyInAnyOrder("value", "key");
        assertThat(AccessGuard.class.getMethod("value").getDefaultValue()).isNull();
        assertThat(AccessGuard.class.getMethod("key").getDefaultValue()).isEqualTo("");
    }

    @Test
    void annotationsExposeOnlyApprovedTargets() {
        assertThat(AccessGuard.class.getAnnotation(Target.class).value())
                .containsExactlyInAnyOrder(ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR);
        assertThat(GuardKey.class.getAnnotation(Target.class).value())
                .containsExactlyInAnyOrder(ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT);
        assertThat(AllowListGuard.class.getAnnotation(Target.class).value()).containsExactly(ElementType.METHOD);
        assertThat(RateLimitGuard.class.getAnnotation(Target.class).value()).containsExactly(ElementType.METHOD);
        assertThat(TimeLimitGuard.class.getAnnotation(Target.class).value()).containsExactly(ElementType.METHOD);
    }

    @Test
    void requestDefensivelyCopiesArgumentsAndAttributes() {
        Object[] arguments = {"user-1"};
        Map<String, Object> attributes = new HashMap<>(Map.of("tenant", "t1"));
        GuardRequest request = new GuardRequest("draw", arguments, attributes, String.class, null);

        arguments[0] = "changed";
        attributes.put("tenant", "changed");
        Object[] returnedArguments = request.arguments();
        returnedArguments[0] = "returned-change";

        assertThat(request.arguments()).containsExactly("user-1");
        assertThat(request.attributes()).containsEntry("tenant", "t1");
        assertThatThrownBy(() -> request.attributes().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectedExceptionContainsOnlyStableOutcomeDetails() {
        GuardOutcome outcome = GuardOutcome.rejected("draw", GuardDecision.RATE_LIMITED, "rate-limit", 7L);

        AccessGuardRejectedException exception = new AccessGuardRejectedException(outcome);

        assertThat(exception.code()).isEqualTo("ACCESS_GUARD_REJECTED");
        assertThat(exception.outcome()).isSameAs(outcome);
        assertThat(exception.getMessage())
                .contains("draw", "RATE_LIMITED", "THROWN")
                .doesNotContain("user-1");
    }
}
