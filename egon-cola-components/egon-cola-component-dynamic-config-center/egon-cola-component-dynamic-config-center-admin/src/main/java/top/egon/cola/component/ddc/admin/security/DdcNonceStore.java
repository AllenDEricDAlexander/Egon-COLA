package top.egon.cola.component.ddc.admin.security;

import java.time.Duration;

@FunctionalInterface
public interface DdcNonceStore {

    boolean markIfAbsent(
            String credentialId,
            String nonce,
            Duration timeToLive);
}
