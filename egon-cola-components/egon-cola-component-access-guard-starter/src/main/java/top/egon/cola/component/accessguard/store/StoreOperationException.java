package top.egon.cola.component.accessguard.store;

public final class StoreOperationException extends RuntimeException {

    public StoreOperationException(String code) {
        super(code);
    }

    public StoreOperationException(String code, Throwable cause) {
        super(code, cause);
    }
}
