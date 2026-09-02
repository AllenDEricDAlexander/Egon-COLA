package top.egon.cola.archetype.source.web.domain.teaching.vos;

import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainException;
import top.egon.cola.archetype.source.web.domain.validators.OrganizationCodeValidator;

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
