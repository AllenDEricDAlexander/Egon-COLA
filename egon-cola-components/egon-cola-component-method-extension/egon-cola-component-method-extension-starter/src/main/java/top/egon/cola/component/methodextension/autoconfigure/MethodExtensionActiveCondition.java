package top.egon.cola.component.methodextension.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class MethodExtensionActiveCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(
            ConditionContext context,
            AnnotatedTypeMetadata metadata
    ) {
        MethodExtensionEngine engine = MethodExtensionConditionSupport
                .effectiveEngine(context.getEnvironment());
        ConditionMessage message = ConditionMessage.forCondition("Method Extension active engine")
                .because("effective engine is " + engine);
        return engine == MethodExtensionEngine.DISABLED
                ? ConditionOutcome.noMatch(message) : ConditionOutcome.match(message);
    }
}
