package top.egon.cola.component.accessguard.config;

import java.util.Optional;

public class DefaultAccessGuardConfigProvider implements AccessGuardConfigProvider {

    @Override
    public Optional<AccessGuardRuleOverride> findMethodOverride(String ruleName, String methodSignature) {
        return Optional.empty();
    }

    @Override
    public Optional<AccessGuardRuleOverride> findGlobalOverride() {
        return Optional.empty();
    }
}
