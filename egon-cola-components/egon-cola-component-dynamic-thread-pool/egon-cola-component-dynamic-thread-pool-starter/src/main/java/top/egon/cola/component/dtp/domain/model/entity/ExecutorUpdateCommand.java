package top.egon.cola.component.dtp.domain.model.entity;

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

    private String appName;

    private String instanceId;

    private String executorName;

    private ExecutorKind executorKind;

    private Integer corePoolSize;

    private Integer maximumPoolSize;

    private Long keepAliveSeconds;

    private Boolean allowCoreThreadTimeOut;

    private Integer concurrencyLimit;

    private String traceId;

    private String requestId;

    private String operator;

    private Long version;

}
