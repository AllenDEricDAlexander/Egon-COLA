package top.egon.fable.facade.dto;

public class Response {

    private boolean success;

    private String code;

    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static Response success() {
        Response response = new Response();
        response.setSuccess(true);
        response.setCode("SUCCESS");
        response.setMessage("success");
        return response;
    }

    public static Response failure(String code, String message) {
        Response response = new Response();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
