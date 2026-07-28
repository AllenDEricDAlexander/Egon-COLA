package top.egon.cola.component.common.enums;

/**
 * Error status contract used by common result and exception records.
 */
public interface ErrorStatus extends IntCodeEnum {

    String getStatus();

    String getMessage();

    default boolean isSuccess() {
        return getCode() == ResultCode.SUCCESS.getCode();
    }
}
