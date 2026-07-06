package top.egon.cola.component.dtp.admin.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

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
    private String traceId = MDC.get("traceId");
    private T data;

    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(Code.SUCCESS.getCode())
                .info(Code.SUCCESS.getInfo())
                .traceId(MDC.get("traceId"))
                .data(data)
                .build();
    }

    public static <T> Response<T> error(String info) {
        return Response.<T>builder()
                .code(Code.ILLEGAL_PARAMETER.getCode())
                .info(info)
                .traceId(MDC.get("traceId"))
                .build();
    }

    public static <T> Response<T> fail(String info) {
        return Response.<T>builder()
                .code(Code.UN_ERROR.getCode())
                .info(info)
                .traceId(MDC.get("traceId"))
                .build();
    }

    public Response<T> setDataValue(T data) {
        this.data = data;
        return this;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public enum Code {
        SUCCESS("0000", "调用成功"),
        UN_ERROR("0001", "调用失败"),
        ILLEGAL_PARAMETER("0002", "非法参数"),
        ;

        private String code;
        private String info;

    }

}
