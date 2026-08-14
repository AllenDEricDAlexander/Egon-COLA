package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.contract.IdentityUserState;

public interface IdentityUserStatePort {

    void publish(IdentityUserState state);
}
