package ${package}.domain.teaching.vos;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;
import ${package}.domain.validators.OrganizationCodeValidator;

public record GradeCode(String value) {
    public GradeCode {
        if (value == null || value.isBlank() || value.length() > 120) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.INVALID_CODE,
                "grade code must not be blank or exceed 120 characters");
        }
    }

    public static GradeCode create(String raw) { return new GradeCode(OrganizationCodeValidator.normalize(raw)); }
}
