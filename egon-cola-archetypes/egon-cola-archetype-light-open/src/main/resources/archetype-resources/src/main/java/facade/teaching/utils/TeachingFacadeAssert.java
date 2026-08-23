package ${package}.facade.teaching.utils;

import ${package}.facade.teaching.exceptions.TeachingFacadeException;

public final class TeachingFacadeAssert {
    private TeachingFacadeAssert() {
    }

    public static <T> T notNull(T value, String code, String message) {
        if (value == null) {
            throw new TeachingFacadeException(code, message);
        }
        return value;
    }
}
