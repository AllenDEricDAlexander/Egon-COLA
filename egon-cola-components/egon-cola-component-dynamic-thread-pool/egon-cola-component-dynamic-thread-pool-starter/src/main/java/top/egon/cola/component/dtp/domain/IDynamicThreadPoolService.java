package top.egon.cola.component.dtp.domain;

import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;

import java.util.List;

/**
 * @author 有罗敷的马同学
 * @description 动态线程池服务
 * @Date 上午8:56 2025/4/13
 **/
public interface IDynamicThreadPoolService {

    List<ExecutorSnapshot> queryExecutorSnapshots();

    ExecutorSnapshot queryExecutorSnapshot(String executorName);

    UpdateResult updateExecutor(ExecutorUpdateCommand command);

}
