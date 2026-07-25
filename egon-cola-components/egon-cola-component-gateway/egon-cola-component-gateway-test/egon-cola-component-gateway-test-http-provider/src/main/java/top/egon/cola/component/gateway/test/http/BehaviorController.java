package top.egon.cola.component.gateway.test.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

@RestController
@RequestMapping("/api")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台域",
        entityDomainCode = "gateway-test",
        entityDomainName = "网关测试实体域",
        code = "failure-and-body",
        name = "故障与报文接口组"
)
public class BehaviorController {

    @GetMapping("/slow/{millis}")
    @GatewayOperation(
            name = "延迟响应",
            externalAccessible = true,
            tags = {"failure-test"}
    )
    public DelayView slow(@PathVariable long millis)
            throws InterruptedException {
        long bounded = Math.max(0, Math.min(millis, 10_000));
        Thread.sleep(bounded);
        return new DelayView(bounded);
    }

    @GetMapping("/fail/{status}")
    @GatewayOperation(
            name = "指定状态失败",
            externalAccessible = true,
            tags = {"failure-test"}
    )
    public ResponseEntity<FailureView> fail(@PathVariable int status) {
        int bounded = status >= 400 && status <= 599 ? status : 500;
        return ResponseEntity.status(bounded)
                .body(new FailureView("HTTP_PROVIDER_FAILURE", bounded));
    }

    @PostMapping("/body/echo")
    @GatewayOperation(
            name = "回显请求体",
            externalAccessible = true,
            tags = {"body", "idempotent"}
    )
    public byte[] echo(@RequestBody byte[] body) {
        return body;
    }

    public record DelayView(long elapsedMillis) {
    }

    public record FailureView(String code, int status) {
    }
}
