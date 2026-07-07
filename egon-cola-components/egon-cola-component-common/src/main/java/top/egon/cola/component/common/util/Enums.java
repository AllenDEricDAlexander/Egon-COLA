package top.egon.cola.component.common.util;

import java.util.Objects;

/**
 * 枚举查找工具，支持按名称和可选编码查找。
 */
public final class Enums {

    private Enums() {
    }

    public static <E extends Enum<E>> E getByName(Class<E> enumType, String name) {
        if (enumType == null || name == null) {
            return null;
        }
        for (E item : enumType.getEnumConstants()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public static <E extends Enum<E>> boolean containsName(Class<E> enumType, String name) {
        return getByName(enumType, name) != null;
    }

    public static <C, E extends Enum<E> & CodeEnum<C>> E getByCode(Class<E> enumType, C code) {
        if (enumType == null) {
            return null;
        }
        for (E item : enumType.getEnumConstants()) {
            if (Objects.equals(item.getCode(), code)) {
                return item;
            }
        }
        return null;
    }
}
