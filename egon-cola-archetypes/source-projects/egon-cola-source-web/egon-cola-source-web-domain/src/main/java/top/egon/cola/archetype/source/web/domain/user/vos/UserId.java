package top.egon.cola.archetype.source.web.domain.user.vos;

import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainException;

public record UserId(Long value) {

    public UserId {
        if (value == null || value <= 0) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.INVALID_USER_ID, "userId must be positive");
        }
    }
}
