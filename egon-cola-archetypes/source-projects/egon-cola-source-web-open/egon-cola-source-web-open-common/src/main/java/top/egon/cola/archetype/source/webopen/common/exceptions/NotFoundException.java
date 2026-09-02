package top.egon.cola.archetype.source.webopen.common.exceptions;

public class NotFoundException extends BizException {
    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
