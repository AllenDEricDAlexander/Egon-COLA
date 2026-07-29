package top.egon.cola.component.accessguard.key.contributor;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.GuardKeyPart;

import java.util.List;

public final class GlobalKeyContributor implements GuardKeyContributor {

    private static final String GLOBAL_TOKEN = "access-guard-global-v2";

    @Override
    public String id() {
        return "GLOBAL";
    }

    @Override
    public List<GuardKeyPart> contribute(GuardInvocation invocation, KeyConfig config) {
        return List.of(new GuardKeyPart("scope", GLOBAL_TOKEN, Integer.MIN_VALUE));
    }
}
