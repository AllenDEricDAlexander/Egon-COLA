package top.egon.cola.platform.rbac3.starter.fixture.provider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.Map;

@RestController
@RequestMapping("/fixture/payments")
public class PaymentFixtureController {

    @GetMapping("/{paymentId}")
    @RequiresPermission("finance:payment:read")
    public Map<String, String> payment(@PathVariable String paymentId) {
        return Map.of("paymentId", paymentId);
    }
}
