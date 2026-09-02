package top.egon.cola.archetype.source.light.facade.user.utils;

import top.egon.cola.archetype.source.light.facade.user.exceptions.UserFacadeException;

public final class UserFacadeAssert {
    private UserFacadeAssert() {
    }

    public static <T> T notNull(T value, String code, String message) {
        if (value == null) {
            throw new UserFacadeException(code, message);
        }
        return value;
    }
}
