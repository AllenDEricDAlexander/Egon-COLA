package top.egon.cola.component.common.core.enums;

/**
 * Base contract for enums that expose a stable integer code and message.
 */
public interface EgonEnum {

    int getCode();

    String getMessage();
}
