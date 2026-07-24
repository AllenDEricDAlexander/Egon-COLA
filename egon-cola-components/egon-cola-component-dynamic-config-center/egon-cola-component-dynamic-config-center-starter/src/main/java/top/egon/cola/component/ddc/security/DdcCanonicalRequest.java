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

public final class DdcCanonicalRequest {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private final String method;

    private final String path;

    private final String canonicalQuery;

    private final long timestamp;

    private final String nonce;

    private final byte[] body;

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

    public String canonicalQuery() {
        return canonicalQuery;
    }

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

    public long timestamp() {
        return timestamp;
    }

    public String nonce() {
        return nonce;
    }

    public String contentSha256() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

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

    private static boolean isUnreserved(int character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9'
                || character == '-'
                || character == '.'
                || character == '_'
                || character == '~';
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private record QueryPart(String key, String value) {
    }
}
