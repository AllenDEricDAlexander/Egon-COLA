package top.egon.cola.component.accessguard.key.contributor;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.GuardKeyPart;

import java.util.List;

public interface GuardKeyContributor {

    String id();

    List<GuardKeyPart> contribute(GuardInvocation invocation, KeyConfig config);
}
