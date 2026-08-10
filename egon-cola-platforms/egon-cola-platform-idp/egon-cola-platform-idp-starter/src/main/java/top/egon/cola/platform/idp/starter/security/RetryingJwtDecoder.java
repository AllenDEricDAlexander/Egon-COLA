package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 在 JWT 解码失败后重建一次远程 JWK 解码器，以加载新发布的密钥标识。
 * 该装饰器只重试一次，并通过同步刷新避免并发请求重复替换同一失败实例。
 *
 * <p>Rebuilds the remote-JWK decoder once after a JWT decoding failure so a newly published key
 * identifier can be loaded. The decorator retries only once and synchronizes refreshes to prevent
 * concurrent requests from repeatedly replacing the same failed instance.</p>
 */
public final class RetryingJwtDecoder implements JwtDecoder {

    /**
     * 创建新 JWT 解码器实例的工厂。
     *
     * <p>Factory that creates fresh JWT decoder instances.</p>
     */
    private final Supplier<JwtDecoder> decoderFactory;

    /**
     * 当前可见的 JWT 解码器实例。
     *
     * <p>Currently visible JWT decoder instance.</p>
     */
    private volatile JwtDecoder decoder;

    /**
     * 创建可刷新解码器，并立即构造首个委托实例。
     *
     * <p>Creates a refreshable decoder and eagerly builds its initial delegate.</p>
     *
     * @param decoderFactory 解码器工厂；decoder factory
     */
    public RetryingJwtDecoder(Supplier<JwtDecoder> decoderFactory) {
        this.decoderFactory = Objects.requireNonNull(
                decoderFactory, "decoderFactory");
        this.decoder = Objects.requireNonNull(
                decoderFactory.get(), "decoderFactory result");
    }

    /**
     * 解码访问令牌；首次失败时刷新解码器并再尝试一次。
     *
     * <p>Decodes an access token, refreshing the decoder and retrying once after the first
     * failure.</p>
     *
     * @param token 原始 JWT；raw JWT
     * @return 已验证并解码的 JWT；validated and decoded JWT
     * @throws JwtException 当刷新后的第二次解码仍失败时；when decoding still fails after the
     *                      refresh
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        JwtDecoder current = decoder;
        try {
            return current.decode(token);
        } catch (JwtException firstFailure) {
            JwtDecoder refreshed = refresh(current);
            try {
                return refreshed.decode(token);
            } catch (JwtException secondFailure) {
                if (secondFailure != firstFailure) {
                    secondFailure.addSuppressed(firstFailure);
                }
                throw secondFailure;
            }
        }
    }

    /**
     * 在失败实例仍为当前实例时原子替换解码器。
     *
     * <p>Atomically replaces the decoder when the failed instance is still current.</p>
     *
     * @param failedDecoder 首次解码失败时使用的实例；instance used for the failed first attempt
     * @return 当前刷新后的解码器；current refreshed decoder
     */
    private synchronized JwtDecoder refresh(JwtDecoder failedDecoder) {
        if (decoder == failedDecoder) {
            decoder = Objects.requireNonNull(
                    decoderFactory.get(), "decoderFactory result");
        }
        return decoder;
    }
}
