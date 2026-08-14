package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

/**
 * Authorization versions supplied by the caller and checked against the user snapshot.
 */
public record TokenVersionsVO(long authVersion, long policyVersion) {

    public TokenVersionsVO {
        if (authVersion < 0 || policyVersion < 0) {
            throw new IllegalArgumentException("authorization versions must not be negative");
        }
    }
}
