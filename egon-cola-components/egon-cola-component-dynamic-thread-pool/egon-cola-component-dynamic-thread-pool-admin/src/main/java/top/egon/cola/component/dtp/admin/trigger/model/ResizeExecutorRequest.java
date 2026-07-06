package top.egon.cola.component.dtp.admin.trigger.model;

import lombok.Getter;
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

    private ExecutorKind executorKind;

    private Integer corePoolSize;

    private Integer maximumPoolSize;

    private Long keepAliveSeconds;

    private Boolean allowCoreThreadTimeOut;

    private String operator;

}
