package top.egon.cola.platform.idp.starter.state;

import top.egon.cola.platform.idp.contract.IdentityUserState;

import java.util.Optional;

/**
 * Reads the current global identity state used for immediate token invalidation.
 */
@FunctionalInterface
public interface IdentityUserStateReader {

    Optional<IdentityUserState> read(String subject);
}
