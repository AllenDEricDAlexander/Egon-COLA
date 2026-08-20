package top.egon.cola.component.common.core.enums;

/**
 * Default business exception codes and messages.
 */
public enum BusinessExceptionEnum implements ErrorStatus {

    SYSTEM_ERROR(1, "系统处理异常:%s"),
    INVALID_PARAM(2, "参数%s为空或者不合法"),
    INTERFACE_CALL_ERROR(3, "接口调用异常,%s"),
    ENTITY_NULL(4, "传入对象为空,%s"),
    REFUSE_ADD(5, "拒绝增加记录,%s"),
    REFUSE_DELETE(6, "拒绝删除记录,%s"),
    REFUSE_MODIFY(7, "拒绝修改记录,%s"),
    REFUSE_FIND(8, "拒绝查询记录,%s"),
    NO_DATA_FOUND(9, "获取不到数据%s"),
    RESULT_IS_NULL(10, "查询结果集为空"),
    UNIQUE_ERROR(11, "字段不可重复 %s"),
    TIMEOUT(12, "访问超时了"),
    EXISTING_RECORD(13, "重复的记录:%s"),
    APP_ID_NOT_EXISTS(14, "开发者账号不存在"),
    CALLBACK_URL_IS_NULL(15, "开发者回调地址为空"),
    OPEN_REQUEST_EXCEPTION(16, "接口平台请求异常"),
    RECORD_NOT_FOUND(32, "%s记录不存在"),
    OPERATION_FAILED(33, "操作失败,%s"),
    USER_DEFINED_MESSAGE(34, "自定义 %s"),
    RESUBMIT_ERROR(36, "表单重复提交");

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
