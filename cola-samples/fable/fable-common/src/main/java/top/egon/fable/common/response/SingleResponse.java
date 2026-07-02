package top.egon.fable.common.response;

public class SingleResponse<T> extends Response {

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> SingleResponse<T> of(T data) {
        SingleResponse<T> response = new SingleResponse<>();
        response.setSuccess(true);
        response.setCode("SUCCESS");
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> SingleResponse<T> fail(String code, String message) {
        SingleResponse<T> response = new SingleResponse<>();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
