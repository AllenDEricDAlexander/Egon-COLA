package top.egon.cola.platform.idp.core.resource;

/**
 * Resource Server 准入或访问策略拒绝请求时抛出的稳定业务异常。
 *
 * <p>Stable business exception raised when a Resource Server admission or access policy denies a
 * request.</p>
 */
public final class ResourceAuthorizationException extends RuntimeException {

    /**
     * 可安全映射到协议响应的稳定错误码。
     *
     * <p>Stable error code that may be safely mapped to a protocol response.</p>
     */
    private final String code;

    /**
     * 创建 Resource 授权异常。
     *
     * <p>Creates a Resource authorization exception.</p>
     *
     * @param code    稳定错误码；stable error code
     * @param message 不包含敏感信息的错误描述；non-sensitive error description
     */
    public ResourceAuthorizationException(String code, String message) {
        super(required(message, "message"));
        this.code = required(code, "code");
    }

    /**
     * 返回稳定错误码。
     *
     * <p>Returns the stable error code.</p>
     *
     * @return 稳定错误码；stable error code
     */
    public String code() {
        return code;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验文本；text to validate
     * @param field 字段名；field name
     * @return 去除首尾空白后的文本；trimmed text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
