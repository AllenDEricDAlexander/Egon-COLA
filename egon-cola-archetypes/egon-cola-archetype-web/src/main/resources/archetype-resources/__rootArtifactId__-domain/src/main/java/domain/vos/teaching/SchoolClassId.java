package ${package}.domain.vos.teaching;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

public record SchoolClassId(String value) {
    public SchoolClassId {
        if (value == null || value.isBlank() || value.length() > 64) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.DOMAIN_REJECTED, "invalid school class id");
        }
    }
}
