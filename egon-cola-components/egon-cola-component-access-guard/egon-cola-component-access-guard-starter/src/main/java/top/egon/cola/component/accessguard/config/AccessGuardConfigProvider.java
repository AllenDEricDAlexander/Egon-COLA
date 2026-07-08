package top.egon.cola.component.accessguard.config;

import java.util.Optional;

public interface AccessGuardConfigProvider {

    Optional<AccessGuardRuleOverride> findMethodOverride(String ruleName, String methodSignature);

    Optional<AccessGuardRuleOverride> findGlobalOverride();
}
