package ${package}.domain.user.validators;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class UserDomainValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private UserDomainValidator() {
    }

    public static String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isBlank() || name.length() > 120) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.INVALID_USER_NAME, "user name must contain 1 to 120 characters");
        }
        return name;
    }

    public static String normalizeEmail(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 160 || !EMAIL.matcher(email).matches()) {
            throw new OrganizationDomainException(OrganizationDomainErrorCode.INVALID_EMAIL, "invalid user email");
        }
        return email;
    }
}
