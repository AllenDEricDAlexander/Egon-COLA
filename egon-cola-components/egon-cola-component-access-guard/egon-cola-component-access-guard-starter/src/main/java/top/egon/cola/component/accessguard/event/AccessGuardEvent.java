package top.egon.cola.component.accessguard.event;

import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.context.AccessGuardDecision;

public record AccessGuardEvent(
        String ruleName,
        String methodSignature,
        String accessKeyHash,
        AccessGuardDecision decision,
        String message
) {

    public static AccessGuardEvent from(AccessGuardContext context) {
        if (context.result() == null) {
            return new AccessGuardEvent(context.ruleName(), context.methodSignature(), context.accessKeyHash(), null, "");
        }
        return new AccessGuardEvent(
                context.result().ruleName(),
                context.methodSignature(),
                context.result().accessKeyHash(),
                context.result().decision(),
                context.result().message()
        );
    }
}
