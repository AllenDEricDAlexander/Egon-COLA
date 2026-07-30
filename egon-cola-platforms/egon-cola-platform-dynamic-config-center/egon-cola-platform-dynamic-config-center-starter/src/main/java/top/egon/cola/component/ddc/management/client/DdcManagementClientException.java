package top.egon.cola.component.ddc.management.client;

public final class DdcManagementClientException extends RuntimeException {

    private final int code;

    private final String status;

    public DdcManagementClientException(int code, String status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public DdcManagementClientException(String status, String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public String status() {
        return status;
    }
}
