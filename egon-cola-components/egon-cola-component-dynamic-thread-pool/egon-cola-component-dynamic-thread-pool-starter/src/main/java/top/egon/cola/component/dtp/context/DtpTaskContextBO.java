package top.egon.cola.component.dtp.context;

import io.micrometer.context.ThreadLocalAccessor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;

/** 仅供本机任务传递的快照，不作为日志或序列化载体。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DtpTaskContextBO {
    @NotNull
    private TraceContext traceContext;
    @NotNull
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Object registrationIdentity;
    @Valid
    @NotNull
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<AccessorValueBO> values = List.of();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    static class AccessorValueBO {
        @NotNull
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private ThreadLocalAccessor<?> accessor;
        private boolean present;
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private Object value;
    }
}
