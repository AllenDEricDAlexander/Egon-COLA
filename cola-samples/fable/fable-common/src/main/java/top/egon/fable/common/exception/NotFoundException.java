package top.egon.fable.common.exception;

public class NotFoundException extends BizException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
