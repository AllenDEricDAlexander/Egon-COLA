package top.egon.cola.component.common.core.enums;

/**
 * Default business exception codes and messages.
 */
public enum BusinessExceptionEnum implements ErrorStatus {

    SYSTEM_ERROR(1, "system error"),
    INVALID_PARAM(2, "parameter is null or invalid"),
    INTERFACE_CALL_ERROR(3, "interface call failed"),
    ENTITY_NULL(4, "input object is null"),
    REFUSE_ADD(5, "add operation refused"),
    REFUSE_DELETE(6, "delete operation refused"),
    REFUSE_MODIFY(7, "update operation refused"),
    REFUSE_FIND(8, "query operation refused"),
    NO_DATA_FOUND(9, "no data found"),
    RESULT_IS_NULL(10, "query result is empty"),
    UNIQUE_ERROR(11, "field must be unique"),
    TIMEOUT(12, "request timed out"),
    EXISTING_RECORD(13, "record already exists"),
    APP_ID_NOT_EXISTS(14, "developer application does not exist"),
    CALLBACK_URL_IS_NULL(15, "developer callback URL is empty"),
    OPEN_REQUEST_EXCEPTION(16, "open platform request failed"),
    RECORD_NOT_FOUND(32, "record does not exist"),
    OPERATION_FAILED(33, "operation failed"),
    USER_DEFINED_MESSAGE(34, "user-defined business error"),
    RESUBMIT_ERROR(36, "request was submitted more than once");

    private final int code;

    private final String message;

    BusinessExceptionEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getStatus() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static BusinessExceptionEnum fromCode(int code) {
        for (BusinessExceptionEnum exception : values()) {
            if (exception.code == code) {
                return exception;
            }
        }
        return null;
    }

    public static BusinessExceptionEnum fromValue(int value) {
        return fromCode(value);
    }
}
