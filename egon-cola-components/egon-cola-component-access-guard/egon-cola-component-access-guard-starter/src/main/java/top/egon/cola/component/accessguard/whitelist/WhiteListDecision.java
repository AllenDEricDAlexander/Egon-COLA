package top.egon.cola.component.accessguard.whitelist;

import top.egon.cola.component.accessguard.annotation.WhiteListMode;

public record WhiteListDecision(boolean passed, boolean bypassGuard, WhiteListMode mode, String reason) {

    public static WhiteListDecision pass(WhiteListMode mode) {
        return new WhiteListDecision(true, mode == WhiteListMode.BYPASS_GUARD, mode, "white list hit");
    }

    public static WhiteListDecision pass(WhiteListMode mode, String reason) {
        return new WhiteListDecision(true, mode == WhiteListMode.BYPASS_GUARD, mode, reason);
    }

    public static WhiteListDecision reject(String reason) {
        return new WhiteListDecision(false, false, WhiteListMode.GATEKEEPER, reason);
    }
}
