package top.egon.cola.component.accessguard.blacklist;

import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

public interface BlacklistService {

    BlacklistStatus status(AccessGuardRule rule, AccessGuardContext context);

    BlacklistStatus incrementRejectAndMaybeBlacklist(AccessGuardRule rule, AccessGuardContext context);

    void remove(String ruleName, String accessKeyHash);
}
