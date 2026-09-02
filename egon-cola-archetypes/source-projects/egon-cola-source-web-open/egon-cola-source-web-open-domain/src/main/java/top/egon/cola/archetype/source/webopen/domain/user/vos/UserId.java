package top.egon.cola.archetype.source.webopen.domain.user.vos;

import top.egon.cola.archetype.source.webopen.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.webopen.domain.exceptions.OrganizationDomainException;

public record UserId(Long value) {

    public UserId {
        if (value == null || value <= 0) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.INVALID_USER_ID, "userId must be positive");
        }
    }
}
