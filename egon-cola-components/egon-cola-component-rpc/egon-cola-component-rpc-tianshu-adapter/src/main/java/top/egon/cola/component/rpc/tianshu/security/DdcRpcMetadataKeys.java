package top.egon.cola.component.rpc.tianshu.security;

import io.grpc.Metadata;

/**
 * Tianshu RPC 鉴权协议使用的固定 Metadata 键。
 * / Fixed Metadata keys used by the Tianshu RPC authentication protocol.
 */
public final class DdcRpcMetadataKeys {

    public static final Metadata.Key<String> ACCESS_KEY = ascii(
            "x-egon-tianshu-access-key");
    public static final Metadata.Key<String> TIMESTAMP = ascii(
            "x-egon-tianshu-timestamp");
    public static final Metadata.Key<String> NONCE = ascii(
            "x-egon-tianshu-nonce");
    public static final Metadata.Key<String> CONTENT_SHA256 = ascii(
            "x-egon-tianshu-content-sha256");
    public static final Metadata.Key<String> SIGNATURE = ascii(
            "x-egon-tianshu-signature");
    public static final Metadata.Key<String> CONTRACT_VERSION = ascii(
            "x-egon-tianshu-contract-version");

    private DdcRpcMetadataKeys() {
    }

    private static Metadata.Key<String> ascii(String name) {
        return Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER);
    }
}
