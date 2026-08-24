package ${package}.domain.teaching.vos;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

public record SchoolClassId(Long value) {
    public SchoolClassId {
        if (value == null || value <= 0) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.DOMAIN_REJECTED, "school class id must be positive");
        }
    }
}
