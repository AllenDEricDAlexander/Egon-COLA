package top.egon.cola.archetype.source.light.facade.teaching.utils;

import top.egon.cola.archetype.source.light.common.exception.TeachingFacadeException;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Facade-boundary guard; the null check itself belongs to the common validation facade. */
public final class TeachingFacadeAssert {
    private TeachingFacadeAssert() {
    }

    public static <T> T notNull(T value, String code, String message) {
        return ValidationUtils.requireNotNull(value, () -> new TeachingFacadeException(code, message));
    }
}
