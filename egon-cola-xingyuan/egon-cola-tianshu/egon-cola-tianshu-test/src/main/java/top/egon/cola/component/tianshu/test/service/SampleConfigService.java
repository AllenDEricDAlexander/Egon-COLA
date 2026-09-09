package top.egon.cola.component.tianshu.test.service;

import org.springframework.stereotype.Service;
import top.egon.cola.component.tianshu.annotation.DdcValue;

@Service
public class SampleConfigService {

    @DdcValue("${downgrade-switch:false}")
    private volatile Boolean downgradeSwitch;

    @DdcValue("${order.rate-limit.permits-per-second:100}")
    private volatile Integer rateLimit;

    public Boolean getDowngradeSwitch() {
        return downgradeSwitch;
    }

    public Integer getRateLimit() {
        return rateLimit;
    }
}
