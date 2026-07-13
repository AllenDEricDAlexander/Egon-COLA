package ${package}.domain.user.vos;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.INVALID_USER_ID, "userId must not be blank");
        }
    }
}
