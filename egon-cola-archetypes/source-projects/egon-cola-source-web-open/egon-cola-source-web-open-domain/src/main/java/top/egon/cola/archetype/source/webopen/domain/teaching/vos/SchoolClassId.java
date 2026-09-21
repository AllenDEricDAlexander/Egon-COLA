package top.egon.cola.archetype.source.webopen.domain.teaching.vos;

import top.egon.cola.archetype.source.webopen.common.enums.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationDomainException;

public record SchoolClassId(Long value) {
    public SchoolClassId {
        if (value == null || value <= 0) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.DOMAIN_REJECTED, "school class id must be positive");
        }
    }
}
