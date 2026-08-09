package top.egon.cola.component.rpc.ddc.security;

import io.grpc.Metadata;

/**
 * DDC RPC 鉴权协议使用的固定 Metadata 键。
 * / Fixed Metadata keys used by the DDC RPC authentication protocol.
 */
public final class DdcRpcMetadataKeys {

    public static final Metadata.Key<String> ACCESS_KEY = ascii(
            "x-egon-ddc-access-key");
    public static final Metadata.Key<String> TIMESTAMP = ascii(
            "x-egon-ddc-timestamp");
    public static final Metadata.Key<String> NONCE = ascii(
            "x-egon-ddc-nonce");
    public static final Metadata.Key<String> CONTENT_SHA256 = ascii(
            "x-egon-ddc-content-sha256");
    public static final Metadata.Key<String> SIGNATURE = ascii(
            "x-egon-ddc-signature");
    public static final Metadata.Key<String> CONTRACT_VERSION = ascii(
            "x-egon-ddc-contract-version");

    private DdcRpcMetadataKeys() {
    }

    private static Metadata.Key<String> ascii(String name) {
        return Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER);
    }
}
