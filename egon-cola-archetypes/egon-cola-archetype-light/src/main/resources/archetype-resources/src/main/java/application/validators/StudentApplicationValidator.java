package ${package}.application.validators;

import ${package}.common.exceptions.BizException;

public final class StudentApplicationValidator {
    private StudentApplicationValidator() {
    }

    public static void requireText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("VALIDATION_ERROR", fieldName + " must not be blank");
        }
    }
}
