package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Rebuilds the remote-JWK decoder once so a newly published kid can be loaded.
 */
public final class RetryingJwtDecoder implements JwtDecoder {

    private final Supplier<JwtDecoder> decoderFactory;
    private volatile JwtDecoder decoder;

    public RetryingJwtDecoder(Supplier<JwtDecoder> decoderFactory) {
        this.decoderFactory = Objects.requireNonNull(
                decoderFactory, "decoderFactory");
        this.decoder = Objects.requireNonNull(
                decoderFactory.get(), "decoderFactory result");
    }

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

    private synchronized JwtDecoder refresh(JwtDecoder failedDecoder) {
        if (decoder == failedDecoder) {
            decoder = Objects.requireNonNull(
                    decoderFactory.get(), "decoderFactory result");
        }
        return decoder;
    }
}
