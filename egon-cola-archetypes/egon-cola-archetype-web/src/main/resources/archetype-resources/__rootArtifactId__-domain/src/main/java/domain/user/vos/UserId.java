package ${package}.domain.user.vos;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

public record UserId(Long value) {

    public UserId {
        if (value == null || value <= 0) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.INVALID_USER_ID, "userId must be positive");
        }
    }
}
