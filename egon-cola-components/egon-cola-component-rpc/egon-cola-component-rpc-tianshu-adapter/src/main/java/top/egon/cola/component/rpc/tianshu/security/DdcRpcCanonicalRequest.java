package top.egon.cola.component.rpc.tianshu.security;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Tianshu RPC HMAC v2 协议的确定性六行请求表示。
 * / Deterministic six-line request representation for Tianshu RPC HMAC v2.
 */
public final class DdcRpcCanonicalRequest {

    public static final String CONTRACT_VERSION = "v2";

    private final long tenantId;
    private final String fullMethodName;
    private final long timestamp;
    private final String nonce;
    private final byte[] deterministicBody;

    public DdcRpcCanonicalRequest(
            long tenantId,
            String fullMethodName,
            long timestamp,
            String nonce,
            Message request) {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        this.tenantId = tenantId;
        this.fullMethodName = require(fullMethodName, "fullMethodName");
        this.timestamp = timestamp;
        this.nonce = require(nonce, "nonce");
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        this.deterministicBody = deterministicBytes(request);
    }

    /**
     * 使用 protobuf 确定性序列化生成签名请求体。
     * / Serializes a protobuf request deterministically for signing.
     *
     * @param message protobuf 消息 / protobuf message
     * @return 确定性字节 / deterministic bytes
     */
    public static byte[] deterministicBytes(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message is required");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(
                    message.getSerializedSize());
            CodedOutputStream output = CodedOutputStream.newInstance(bytes);
            output.useDeterministicSerialization();
            message.writeTo(output);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Protobuf deterministic serialization failed",
                    exception
            );
        }
    }

    /** 返回小写 SHA-256 请求摘要。 / Returns the lowercase request SHA-256 digest. */
    public String contentSha256() {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(deterministicBody)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    /** 返回无尾部换行的六行规范文本。 / Returns the six-line canonical text without a trailing LF. */
    public String canonicalValue() {
        return String.join(
                "\n",
                CONTRACT_VERSION,
                Long.toString(tenantId),
                fullMethodName,
                Long.toString(timestamp),
                nonce,
                contentSha256()
        );
    }

    /** 返回规范文本的 UTF-8 字节。 / Returns UTF-8 bytes of the canonical text. */
    public byte[] canonicalBytes() {
        return canonicalValue().getBytes(StandardCharsets.UTF_8);
    }

    public long timestamp() {
        return timestamp;
    }

    public long tenantId() {
        return tenantId;
    }

    public String nonce() {
        return nonce;
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
