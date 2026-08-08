package top.egon.cola.component.ddc.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 构造 DDC HMAC 协议使用的确定性请求表示。 Builds the deterministic request representation used by the DDC HMAC protocol.
 */
public final class DdcCanonicalRequest {

    /** 百分号编码使用的大写十六进制字符表。 Uppercase hexadecimal alphabet used by percent encoding. */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /** 规范化为大写的 HTTP 方法。 HTTP method normalized to uppercase. */
    private final String method;

    /** 请求路径。 Request path. */
    private final String path;

    /** 按编码后的键和值排序的规范查询字符串。 Canonical query string sorted by encoded key and value. */
    private final String canonicalQuery;

    /** 请求时间戳，单位由签名协议约定。 Request timestamp in the unit defined by the signing protocol. */
    private final long timestamp;

    /** 用于防止重放的请求 nonce。 Request nonce used to prevent replay. */
    private final String nonce;

    /** 为避免外部修改而复制的请求体字节。 Request-body bytes copied to prevent external mutation. */
    private final byte[] body;

    /**
     * 规范化请求方法和查询参数，并防御性复制请求体。 Normalizes method and query parameters and defensively copies the body.
     * @param method HTTP 方法。 HTTP method
     * @param path 请求路径。 request path
     * @param query 多值查询参数。 multi-valued query parameters
     * @param timestamp 请求时间戳。 request timestamp
     * @param nonce 请求唯一随机值。 unique request nonce
     * @param body 请求体字节，空引用按空字节处理。 body bytes, with null treated as empty
     * @throws IllegalArgumentException 方法、路径或 nonce 为空白时抛出。 thrown when method, path, or nonce is blank
     */
    public DdcCanonicalRequest(String method,
                               String path,
                               Map<String, ? extends Collection<String>> query,
                               long timestamp,
                               String nonce,
                               byte[] body) {
        this.method = require(method, "method").toUpperCase(Locale.ROOT);
        this.path = require(path, "path");
        this.canonicalQuery = canonicalizeQuery(query == null ? Map.of() : query);
        this.timestamp = timestamp;
        this.nonce = require(nonce, "nonce");
        this.body = body == null ? new byte[0] : body.clone();
    }

    /**
     * 返回编码并排序后的规范查询字符串。 Returns the encoded and sorted canonical query string.
     * @return 规范查询字符串。 canonical query string
     */
    public String canonicalQuery() {
        return canonicalQuery;
    }

    /**
     * 展开多值参数、百分号编码并按键和值排序。 Expands multi-valued parameters, percent-encodes them, and sorts by key and value.
     * @param query 多值查询参数。 multi-valued query parameters
     * @return 不带问号的规范查询字符串。 canonical query string without a question mark
     */
    private static String canonicalizeQuery(Map<String, ? extends Collection<String>> query) {
        List<QueryPart> parts = new ArrayList<>();
        query.forEach((key, values) -> {
            Collection<String> normalizedValues =
                    values == null || values.isEmpty() ? List.of("") : values;
            for (String value : normalizedValues) {
                parts.add(new QueryPart(percentEncode(key), percentEncode(value)));
            }
        });
        parts.sort(Comparator.comparing(QueryPart::key).thenComparing(QueryPart::value));
        return parts.stream()
                .map(part -> part.key() + "=" + part.value())
                .collect(Collectors.joining("&"));
    }

    /**
     * 返回参与签名的请求时间戳。 Returns the request timestamp included in signing.
     * @return 请求时间戳。 request timestamp
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * 返回参与签名和防重放校验的 nonce。 Returns the nonce used for signing and replay protection.
     * @return 请求 nonce。 request nonce
     */
    public String nonce() {
        return nonce;
    }

    /**
     * 计算请求体 SHA-256 摘要。 Computes the SHA-256 digest of the request body.
     * @return 小写十六进制内容摘要。 lowercase hexadecimal content digest
     * @throws IllegalStateException 当前 JRE 不提供 SHA-256 时抛出。 thrown when SHA-256 is unavailable in the current JRE
     */
    public String contentSha256() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    /**
     * 以换行连接方法、路径、查询、时间戳、nonce 和内容摘要。 Joins method, path, query, timestamp, nonce, and content digest with newlines.
     * @return 用于 HMAC 的规范请求文本。 canonical request text used for HMAC
     */
    public String canonicalValue() {
        return String.join(
                "\n",
                method,
                path,
                canonicalQuery(),
                Long.toString(timestamp),
                nonce,
                contentSha256()
        );
    }

    /**
     * 将 UTF-8 字节按未保留字符规则进行百分号编码。 Percent-encodes UTF-8 bytes while preserving unreserved characters.
     * @param value 待编码文本，空引用按空字符串处理。 text to encode, with null treated as empty
     * @return 使用大写十六进制的编码文本。 encoded text using uppercase hexadecimal digits
     */
    private static String percentEncode(String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length);
        for (byte valueByte : bytes) {
            int character = Byte.toUnsignedInt(valueByte);
            if (isUnreserved(character)) {
                encoded.append((char) character);
            } else {
                encoded.append('%')
                        .append(HEX[character >>> 4])
                        .append(HEX[character & 0x0F]);
            }
        }
        return encoded.toString();
    }

    /**
     * 判断 ASCII 字节是否属于 URI 未保留字符。 Determines whether an ASCII byte is an unreserved URI character.
     * @param character 无符号字节值。 unsigned byte value
     * @return 字母、数字或 {@code -._~} 时为 {@code true}。 {@code true} for letters, digits, or {@code -._~}
     */
    private static boolean isUnreserved(int character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9'
                || character == '-'
                || character == '.'
                || character == '_'
                || character == '~';
    }

    /**
     * 要求签名字段包含非空白文本。 Requires a signing field to contain non-whitespace text.
     * @param value 字段值。 field value
     * @param fieldName 用于错误消息的字段名。 field name used in the error message
     * @return 原字段值。 original field value
     * @throws IllegalArgumentException 字段为空或空白时抛出。 thrown when the field is null or blank
     */
    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    /**
     * 保存一个已编码查询键值对，供稳定排序和拼接。 Holds one encoded query pair for stable sorting and joining.
     * @param key 已编码查询键。 encoded query key
     * @param value 已编码查询值。 encoded query value
     */
    private record QueryPart(String key, String value) {
    }
}
