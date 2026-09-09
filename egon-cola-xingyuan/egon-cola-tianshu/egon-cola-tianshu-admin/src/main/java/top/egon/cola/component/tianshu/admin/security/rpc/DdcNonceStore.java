package top.egon.cola.component.tianshu.admin.security.rpc;

import java.time.Duration;

@FunctionalInterface
public interface DdcNonceStore {

    boolean markIfAbsent(
            String credentialId,
            String nonce,
            Duration timeToLive);
}
