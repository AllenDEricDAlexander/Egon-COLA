package top.egon.cola.archetype.source.web.common.exception;

public class NotFoundException extends BizException {
    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
