package top.egon.light.common.response;

public class Response {
    private final boolean success;
    private final String code;
    private final String message;

    public Response(boolean success, String code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    public static Response success() {
        return new Response(true, "SUCCESS", "success");
    }

    public static Response fail(String code, String message) {
        return new Response(false, code, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
