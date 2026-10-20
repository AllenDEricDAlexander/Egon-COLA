package top.egon.cola.component.dtp.domain.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

/**
 * @author      有罗敷的马同学
 * @description 执行器更新命令实体对象
 * @Date        上午8:55 2026/6/29
 **/
@Getter
@Setter
@ToString
public class ExecutorUpdateCommand {

    public interface PlatformResize { }

    public interface VirtualLimit { }

    private String appName;

    private String instanceId;

    private String executorName;

    private ExecutorKind executorKind;

    @Positive(groups = PlatformResize.class)
    private Integer corePoolSize;

    @Positive(groups = PlatformResize.class)
    private Integer maximumPoolSize;

    @PositiveOrZero(groups = PlatformResize.class)
    private Long keepAliveSeconds;

    private Boolean allowCoreThreadTimeOut;

    @Positive(groups = VirtualLimit.class)
    private Integer concurrencyLimit;

    @Positive(groups = PlatformResize.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer queueCapacity;

    private String operator;

    private Long version;

    @JsonIgnore
    @AssertTrue(message = "corePoolSize and maximumPoolSize must be provided together and core <= max",
            groups = PlatformResize.class)
    public boolean isPoolSizeValid() {
        return corePoolSize == null && maximumPoolSize == null
                || corePoolSize != null && maximumPoolSize != null && corePoolSize <= maximumPoolSize;
    }

}
