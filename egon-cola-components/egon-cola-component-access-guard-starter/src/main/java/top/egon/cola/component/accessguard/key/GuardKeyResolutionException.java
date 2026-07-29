package top.egon.cola.component.accessguard.key;

public final class GuardKeyResolutionException extends RuntimeException {

    private final String code;

    public GuardKeyResolutionException(String code) {
        super("Access Guard key resolution failed: " + code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
