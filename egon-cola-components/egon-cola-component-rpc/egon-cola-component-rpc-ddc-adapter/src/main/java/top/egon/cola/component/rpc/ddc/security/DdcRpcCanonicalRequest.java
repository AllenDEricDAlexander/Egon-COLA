package top.egon.cola.component.rpc.ddc.security;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * DDC RPC HMAC 协议的确定性五行请求表示。
 * / Deterministic five-line request representation for DDC RPC HMAC.
 */
public final class DdcRpcCanonicalRequest {

    public static final String CONTRACT_VERSION = "v1";

    private final String fullMethodName;
    private final long timestamp;
    private final String nonce;
    private final byte[] deterministicBody;

    public DdcRpcCanonicalRequest(
            String fullMethodName,
            long timestamp,
            String nonce,
            Message request) {
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

    /** 返回无尾部换行的五行规范文本。 / Returns the five-line canonical text without a trailing LF. */
    public String canonicalValue() {
        return String.join(
                "\n",
                CONTRACT_VERSION,
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
