package top.egon.fable.web.common.exceptions;

public class NotFoundException extends BizException {
    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
