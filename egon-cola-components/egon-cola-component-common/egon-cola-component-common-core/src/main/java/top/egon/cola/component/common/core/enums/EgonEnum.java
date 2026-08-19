package top.egon.cola.component.common.core.enums;

/**
 * Base contract for enums that expose a stable integer code and message.
 *
 * <p>{@link Enum} supplies the final {@link Enum#ordinal()} and {@link Enum#name()}
 * implementations to every enum constant. The compiler supplies the concrete
 * enum type's {@code values()} and {@code valueOf(String)} methods, so the
 * interface exposes type-safe class-aware helpers for those two operations.</p>
 */
public interface EgonEnum {

    int getCode();

    String getMessage();

    int ordinal();

    String name();

    static <E extends Enum<E> & EgonEnum> E[] values(Class<E> enumType) {
        return enumType.getEnumConstants();
    }

    static <E extends Enum<E> & EgonEnum> E valueOf(Class<E> enumType, String name) {
        return Enum.valueOf(enumType, name);
    }
}
