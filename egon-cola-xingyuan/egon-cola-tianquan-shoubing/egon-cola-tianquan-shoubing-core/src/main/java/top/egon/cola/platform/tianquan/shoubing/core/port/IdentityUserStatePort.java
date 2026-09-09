package top.egon.cola.platform.tianquan.shoubing.core.port;

import top.egon.cola.platform.tianquan.shoubing.contract.IdentityUserState;

public interface IdentityUserStatePort {

    void publish(IdentityUserState state);
}
