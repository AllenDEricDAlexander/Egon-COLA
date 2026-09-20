package top.egon.cola.component.dtp.admin.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    private static final long serialVersionUID = -2474596551402989285L;

    private String code;
    private String info;
    @Builder.Default
    private String traceId = TraceContext.getTraceId();
    private T data;

    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(Code.SUCCESS.getStatus())
                .info(Code.SUCCESS.getMessage())
                .traceId(TraceContext.getTraceId())
                .data(data)
                .build();
    }

    public static <T> Response<T> error(String info) {
        return Response.<T>builder()
                .code(Code.ILLEGAL_PARAMETER.getStatus())
                .info(info)
                .traceId(TraceContext.getTraceId())
                .build();
    }

    public static <T> Response<T> fail(String info) {
        return Response.<T>builder()
                .code(Code.UN_ERROR.getStatus())
                .info(info)
                .traceId(TraceContext.getTraceId())
                .build();
    }

    public Response<T> setDataValue(T data) {
        this.data = data;
        return this;
    }

    public enum Code implements ErrorStatus {
        SUCCESS(0, "0000", "调用成功"),
        UN_ERROR(1, "0001", "调用失败"),
        ILLEGAL_PARAMETER(2, "0002", "非法参数"),
        ;

        private final int code;

        private final String status;

        private final String message;

        Code(int code, String status, String message) {
            this.code = code;
            this.status = status;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public String getMessage() {
            return message;
        }

    }

}
