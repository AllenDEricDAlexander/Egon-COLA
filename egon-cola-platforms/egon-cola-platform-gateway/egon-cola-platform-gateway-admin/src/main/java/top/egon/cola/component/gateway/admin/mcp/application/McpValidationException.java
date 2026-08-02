package top.egon.cola.component.gateway.admin.mcp.application;

public final class McpValidationException extends RuntimeException {

    private final String code;

    private final String path;

    public McpValidationException(String code, String path, String message) {
        super(message);
        this.code = code;
        this.path = path;
    }

    public String code() {
        return code;
    }

    public String path() {
        return path;
    }
}
