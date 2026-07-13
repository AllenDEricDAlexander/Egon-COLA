package ${package}.domain.teaching.validators;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

public final class TeachingDomainValidator {
    private TeachingDomainValidator() {}

    public static String normalizeName(String name, String field) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > 120) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.DOMAIN_REJECTED, field + " must not be blank or exceed 120 characters");
        }
        return normalized;
    }
}
