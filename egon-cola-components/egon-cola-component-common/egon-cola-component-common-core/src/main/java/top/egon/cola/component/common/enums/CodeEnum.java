package top.egon.cola.component.common.enums;

/**
 * Base contract for enums that expose a stable code value.
 */
public interface CodeEnum<C> {

    C getCode();
}
