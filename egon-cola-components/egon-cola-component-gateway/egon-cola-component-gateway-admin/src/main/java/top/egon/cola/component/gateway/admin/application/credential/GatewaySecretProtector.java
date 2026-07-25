package top.egon.cola.component.gateway.admin.application.credential;

public interface GatewaySecretProtector {

    ProtectedSecret protect(String plaintext, String associatedData);

    String unprotect(ProtectedSecret secret, String associatedData);

    record ProtectedSecret(String ciphertext, String keyVersion) {
    }
}
