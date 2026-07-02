package top.egon.fable-web.common.response;

public record SingleResponse<T>(boolean success, String code, String message, T data) {
    public static <T> SingleResponse<T> of(T data) {
        return new SingleResponse<>(true, "SUCCESS", "success", data);
    }

    public static <T> SingleResponse<T> fail(String code, String message) {
        return new SingleResponse<>(false, code, message, null);
    }
}
