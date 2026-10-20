package top.egon.cola.component.dtp.admin.trigger.model;

import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import lombok.Setter;
import lombok.ToString;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

/**
 * @author      有罗敷的马同学
 * @description 执行器线程池容量调整请求
 * @Date        上午12:20 2026/6/30
 **/
@Getter
@Setter
@ToString
public class ResizeExecutorRequest {

    @NotNull(groups = ExecutorUpdateCommand.PlatformResize.class)
    private ExecutorKind executorKind;

    @NotNull(groups = ExecutorUpdateCommand.PlatformResize.class)
    @Positive(groups = ExecutorUpdateCommand.PlatformResize.class)
    private Integer corePoolSize;

    @NotNull(groups = ExecutorUpdateCommand.PlatformResize.class)
    @Positive(groups = ExecutorUpdateCommand.PlatformResize.class)
    private Integer maximumPoolSize;

    @PositiveOrZero(groups = ExecutorUpdateCommand.PlatformResize.class)
    private Long keepAliveSeconds;

    private Boolean allowCoreThreadTimeOut;

    private String operator;

    @Positive(groups = ExecutorUpdateCommand.PlatformResize.class)
    @Schema(description = "Optional actual queue capacity; only supported resizable queues, not below live backlog", nullable = true, minimum = "1")
    private Integer queueCapacity;

}
