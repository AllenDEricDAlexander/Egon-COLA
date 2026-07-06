package top.egon.cola.component.dtp.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

import java.time.Instant;

/**
 * @author      有罗敷的马同学
 * @description 执行器快照实体对象
 * @Date        上午8:55 2026/6/29
 **/
@Getter
@Setter
@ToString
public class ExecutorSnapshot {

    private String appName;

    private String instanceId;

    private String executorName;

    private ExecutorKind executorKind;

    private boolean virtual;

    private boolean resizable;

    private Integer corePoolSize;

    private Integer maximumPoolSize;

    private Integer activeCount;

    private Integer poolSize;

    private Long taskCount;

    private Long completedTaskCount;

    private String queueType;

    private Integer queueSize;

    private Integer remainingCapacity;

    private Integer concurrencyLimit;

    private Long runningTasks;

    private Integer availablePermits;

    private Long submittedTasks;

    private Long failedTasks;

    private Long rejectedTasks;

    private Instant reportTime;

}
