package top.egon.cola.component.accessguard.whitelist;

import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.config.AccessGuardRule;

import java.util.List;

public class DefaultWhiteListService implements WhiteListService {

    private final AccessGuardProperties properties;

    private final WhiteListRepository whiteListRepository;

    public DefaultWhiteListService(AccessGuardProperties properties, WhiteListRepository whiteListRepository) {
        this.properties = properties;
        this.whiteListRepository = whiteListRepository;
    }

    @Override
    public WhiteListDecision check(AccessGuardRule rule, String accessKeyHash) {
        if (!rule.whiteListEnabled()) {
            return WhiteListDecision.pass(rule.whiteListMode(), "white list disabled");
        }

        boolean hasAnySource = hasUsers(rule.whiteListUsers())
                || whiteListRepository.contains(rule.name(), accessKeyHash)
                || hasUsers(properties.getWhiteList().getDefaultUsers());
        if (!hasAnySource
                && properties.getWhiteList().getEmptyListStrategy() == AccessGuardProperties.WhiteListEmptyListStrategy.DENY_ALL) {
            return WhiteListDecision.reject("white list is empty");
        }

        if (contains(rule.whiteListUsers(), accessKeyHash)
                || whiteListRepository.contains(rule.name(), accessKeyHash)
                || contains(properties.getWhiteList().getDefaultUsers(), accessKeyHash)) {
            return WhiteListDecision.pass(rule.whiteListMode());
        }
        return WhiteListDecision.reject("access key is not in white list");
    }

    private boolean hasUsers(List<String> users) {
        return users != null && !users.isEmpty();
    }

    private boolean contains(List<String> users, String accessKeyHash) {
        return users != null && users.contains(accessKeyHash);
    }
}
