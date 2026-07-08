package top.egon.cola.component.accessguard.whitelist;

import top.egon.cola.component.accessguard.config.AccessGuardRule;

public interface WhiteListService {

    WhiteListDecision check(AccessGuardRule rule, String accessKeyHash);
}
