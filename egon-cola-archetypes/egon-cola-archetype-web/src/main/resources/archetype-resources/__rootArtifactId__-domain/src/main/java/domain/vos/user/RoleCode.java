package ${package}.domain.vos.user;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

import java.util.Locale;

public record RoleCode(String value) {

    public RoleCode {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new OrganizationDomainException(OrganizationDomainErrorCode.INVALID_CODE, "invalid role code");
        }
        value = normalized;
    }
}
