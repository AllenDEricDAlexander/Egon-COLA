package top.egon.cola.archetype.source.light.facade.user.utils;

import top.egon.cola.archetype.source.light.common.exception.UserFacadeException;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Facade-boundary guard; the null check itself belongs to the common validation facade. */
public final class UserFacadeAssert {
    private UserFacadeAssert() {
    }

    public static <T> T notNull(T value, String code, String message) {
        return ValidationUtils.requireNotNull(value, () -> new UserFacadeException(code, message));
    }
}
