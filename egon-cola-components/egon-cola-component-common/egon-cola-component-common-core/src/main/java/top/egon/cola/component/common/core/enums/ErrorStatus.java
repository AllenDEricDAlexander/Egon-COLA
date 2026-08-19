package top.egon.cola.component.common.core.enums;

/**
 * Error status contract used by common result and exception records.
 */
public interface ErrorStatus extends EgonEnum {

    String getStatus();

    default boolean isSuccess() {
        return getCode() == ResultCode.SUCCESS.getCode();
    }
}
