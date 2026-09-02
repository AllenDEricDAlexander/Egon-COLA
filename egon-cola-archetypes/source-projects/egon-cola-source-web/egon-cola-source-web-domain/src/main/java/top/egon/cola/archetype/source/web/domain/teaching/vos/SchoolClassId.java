package top.egon.cola.archetype.source.web.domain.teaching.vos;

import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainException;

public record SchoolClassId(Long value) {
    public SchoolClassId {
        if (value == null || value <= 0) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.DOMAIN_REJECTED, "school class id must be positive");
        }
    }
}
