package top.egon.cola.platform.tianquan.shoubing.starter.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;

/** Reads the authoritative shared OAuth security snapshot for each verified access token. */
@FunctionalInterface
public interface OAuthRevocationReader {

    Result check(@NotNull @Valid VerifiedJwtBO verifiedJwt);

    enum Status {
        VALID,
        REVOKED,
        UNAVAILABLE
    }

    record Result(Status status, String reasonCode) {

        public Result {
            status = Objects.requireNonNull(status, "status");
            if (reasonCode == null || !reasonCode.matches("[A-Z0-9_]{1,96}")) {
                throw new IllegalArgumentException("reasonCode is invalid");
            }
        }

        public static Result valid() {
            return new Result(Status.VALID, "OAUTH_SECURITY_CURRENT");
        }

        public static Result revoked(String reasonCode) {
            return new Result(Status.REVOKED, reasonCode);
        }

        public static Result unavailable(String reasonCode) {
            return new Result(Status.UNAVAILABLE, reasonCode);
        }
    }
}
