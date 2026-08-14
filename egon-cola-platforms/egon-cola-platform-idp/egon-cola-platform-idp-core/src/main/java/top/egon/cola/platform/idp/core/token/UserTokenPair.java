package top.egon.cola.platform.idp.core.token;

import java.time.Instant;

/**
 * In-process result used to place tokens into transport-specific cookies.
 * 仅供进程内写入传输层 Cookie 的 Token 结果，不是 Controller 响应体。
 */
public record UserTokenPair(
        String accessToken,
        String refreshToken,
        Instant accessExpiresAt,
        Instant refreshExpiresAt
) {

    @Override
    public String toString() {
        return "UserTokenPair[accessToken=<redacted>, refreshToken=<redacted>"
                + ", accessExpiresAt=" + accessExpiresAt
                + ", refreshExpiresAt=" + refreshExpiresAt + ']';
    }
}
