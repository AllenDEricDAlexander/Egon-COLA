package top.egon.cola.component.dtp.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author      有罗敷的马同学
 * @description 执行器更新结果实体对象
 * @Date        上午8:55 2026/6/29
 **/
@Getter
@Setter
@ToString
public class UpdateResult {

    private boolean success;

    private String message;

    private ExecutorSnapshot before;

    private ExecutorSnapshot after;

}
