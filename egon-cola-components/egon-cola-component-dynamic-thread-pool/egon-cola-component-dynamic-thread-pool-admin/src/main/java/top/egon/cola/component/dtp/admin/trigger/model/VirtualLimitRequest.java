package top.egon.cola.component.dtp.admin.trigger.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author      有罗敷的马同学
 * @description 虚拟线程执行器并发限制调整请求
 * @Date        上午12:20 2026/6/30
 **/
@Getter
@Setter
@ToString
public class VirtualLimitRequest {

    private Integer concurrencyLimit;

    private String operator;

}
