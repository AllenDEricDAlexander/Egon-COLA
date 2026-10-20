package top.egon.cola.component.dtp.domain.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * @description 命名线程池创建命令，尺寸必须显式指定
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class ThreadPoolCreateCommand {

    public interface Create { }

    @NotBlank(groups = Create.class)
    private String executorName;

    @NotNull(groups = Create.class)
    @Min(value = 1, groups = Create.class)
    private Integer corePoolSize;

    @NotNull(groups = Create.class)
    @Min(value = 1, groups = Create.class)
    private Integer maximumPoolSize;

    @NotNull(groups = Create.class)
    @Min(value = 0, groups = Create.class)
    private Integer queueCapacity;

    @Builder.Default
    @NotNull(groups = Create.class)
    private Duration keepAlive = Duration.ofSeconds(60);

    @Builder.Default
    private boolean allowCoreThreadTimeOut = false;

    private ThreadFactory threadFactory;

    private RejectedExecutionHandler rejectedExecutionHandler;

    @JsonIgnore
    @AssertTrue(message = "invalid core/max/keepAlive configuration", groups = Create.class)
    public boolean isConfigurationValid() {
        return (corePoolSize == null || maximumPoolSize == null || corePoolSize <= maximumPoolSize)
                && (keepAlive == null || !keepAlive.isNegative() && keepAlive.getNano() == 0
                && keepAlive.getSeconds() <= Integer.MAX_VALUE
                && (!allowCoreThreadTimeOut || !keepAlive.isZero()));
    }
}
