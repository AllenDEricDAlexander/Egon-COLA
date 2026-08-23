package ${package}.domain.validators;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class OrganizationCodeValidator {

    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    private OrganizationCodeValidator() {
    }

    public static String normalize(String raw) {
        String normalized = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(normalized).matches()) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.INVALID_CODE,
                "code must match [A-Z][A-Z0-9_]{1,63}");
        }
        return normalized;
    }
}
