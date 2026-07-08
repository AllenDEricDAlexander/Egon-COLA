package top.egon.cola.component.common.core.code;

/**
 * Error status contract used by common result and exception models.
 */
public interface ErrorStatus {

    int getCode();

    String getStatus();

    String getMessage();

    default boolean isSuccess() {
        return getCode() == CommonStatus.SUCCESS.getCode();
    }
}
