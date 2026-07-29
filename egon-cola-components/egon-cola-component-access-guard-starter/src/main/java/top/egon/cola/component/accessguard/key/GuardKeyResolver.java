package top.egon.cola.component.accessguard.key;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;

public interface GuardKeyResolver {

    GuardKeyResolution resolve(GuardInvocation invocation, KeyConfig config);
}
